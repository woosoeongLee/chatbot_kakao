

package com.example.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@RestController
@Slf4j
public class KKORestAPI {
    //카카오톡 오픈빌더로 리턴할 스킬 API
    private static final String[]imageUrl = {"https://problematicman1.s3.ap-northeast-2.amazonaws.com/1_10.jpeg",
            "https://problematicman1.s3.ap-northeast-2.amazonaws.com/2_l(%E1%84%89%E1%85%A9%E1%84%86%E1%85%AE%E1%86%AB%E1%84%8C%E1%85%A1).jpeg",
            "https://problematicman1.s3.ap-northeast-2.amazonaws.com/3_7.jpeg",
            "https://problematicman1.s3.ap-northeast-2.amazonaws.com/4_6.jpg",
            "https://problematicman1.s3.ap-northeast-2.amazonaws.com/5_89.png",
            "https://problematicman1.s3.ap-northeast-2.amazonaws.com/6_410.jpeg"};

    private static final String[]answer = {"10","l","7","6","89","410"};

    private int ENDPOINT = 5;
    private static final String helpText = "1.문제를 푸시려면 \"문제\"를 입력해주세요.\n\n" +
                                        "2.문제를 받 \"문제\"를 또 입력하 다음 문제로 넘어갑니다.\n\n" +
                                        "3.도움말을 다시 보고 싶은 경우 \"도움말\"을 입력해주세요.\n\n" +
                                        "4.모두 주관식 답이며 한 문제당 총 5번의 기회가 주어집니다.\n\n";

    private static final String guide2ProblemText="문제를 푸시려면 대화 입력란에 \"문제\"를 입력해주세요.\n";

    private static final String alertString = "문제 사진을 불러올 수 없습니다.";
    private static final String csvFileName = "wrongrate.csv";

    private static final int totalQuestionRow=6;
    private static final int totalQuestionColum=2;

    private String[][]rateCache;

    HashMap<String,Client>accessClientList;

    public KKORestAPI() {
        rateCache = new String[totalQuestionRow][totalQuestionColum];
        accessClientList=new HashMap<>();
        readCSV();
        CSVlog();
    }

    public void CSVlog(){
        System.out.println("csv log");
        for(int i=0;i<rateCache.length;++i){
            System.out.print(i+1+" : ");
            for(int j=0;j<rateCache[0].length;++j){
                System.out.print(rateCache[i][j]+" ");
            }
            System.out.println("");
        }
    }

    public String getUserID(Map<String,Object>params){
        HashMap<String,Object>userRequest = (HashMap<String,Object>)params.get("userRequest");
        HashMap<String,Object>user = (HashMap<String,Object>)userRequest.get("user");
        return (String)user.get("id");
    }

    public String getRateText(int questionNumber) {
        float rightRate = Float.parseFloat(rateCache[questionNumber][0]);
        float wrongRate = Float.parseFloat(rateCache[questionNumber][1]);

        float rate = ((rightRate)/(rightRate+wrongRate))*100;
        String rateString = "정답률 : ".concat(String.format("%.1f",rate)).concat("%");
        return rateString;
    }
/*
 * ObjectMapper mapper=new ObjectMapper();
 * String jsonInString=mapper.writevakyeAsString(params);*/
    HashMap<String,Object> setQuestion(int questionNumber,List<String>addedText){
        HashMap<String,Object>resultJson = new HashMap<>();

        try {
            List<Map<String,Object>>outputs = new ArrayList<>();
            HashMap<String,Object>template = new HashMap<>();
            HashMap<String,Object>simpleImage = new HashMap<String, Object>();

            //이미지 부분
            HashMap<String,String>simpleImageFactor = new HashMap<>();

            simpleImageFactor.put("imageUrl",imageUrl[questionNumber]);
            simpleImageFactor.put("altText",alertString);

            simpleImage.put("simpleImage",simpleImageFactor);

            //텍스트
            List<Map<String,Object>>simpleText = new ArrayList<>();
            List<Map<String,Object>>text = new ArrayList<>();

            for (int i = 0;i < addedText.size();++i) {
                text.add(Map.of("text", addedText.get(i)));
                simpleText.add(Map.of("simpleText",text.get(i)));
            }

            if (addedText.size() == 1) {
                outputs.add(simpleImage);
                outputs.add(simpleText.get(0));
            } else {
                outputs.add(simpleText.get(0));
                outputs.add(simpleImage);
                outputs.add(simpleText.get(1));
            }
            template.put("outputs",outputs);

            resultJson.put("version","2.0");
            resultJson.put("template",template);

        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("question error!");
        }
        return resultJson;
    }

    @RequestMapping(value = "/kkoChat/question" , method= {RequestMethod.POST}, headers = {"Accept=application/json"})
    public HashMap<String, Object> responseAPI(@RequestBody Map<String,Object>params, HttpServletRequest request, HttpServletResponse response) {
        int questionNumber = 0;
        String userId = getUserID(params);

        HashMap<String,Object>userRequest = (HashMap<String, Object>) params.get("userRequest");
        String utterance = ((String)userRequest.get("utterance")).trim();

        if (!accessClientList.containsKey(userId)) { // 처음 접근
            if (utterance.equals("문제")) {
                accessClientList.put(userId,new Client());
                accessClientList.get(userId).setCurrentTime();

                accessClientList.get(userId).incrementCurrentIdx(); //문제 내기 전에만 증가
                questionNumber = accessClientList.get(userId).getCurrentQuestionNumber();

                List<String>addedText = new ArrayList<>();
                addedText.add(getRateText(questionNumber));
                return setQuestion(questionNumber,addedText);
            } else {
                return setPlaintext(guide2ProblemText);
            }
        }

        if (accessClientList.get(userId).getPureCurrentQuestionNumber()==ENDPOINT) {
            questionNumber = accessClientList.get(userId).getCurrentQuestionNumber();
            if (answer[questionNumber].equals(utterance)) {
               accessClientList.get(userId).incrementSuccessTime();
            }

            StringBuffer alertEndOfGame = new StringBuffer().append(6).append("문제 중 ")
                    .append(accessClientList.get(userId).getSuccessTime()).append("문제 성공!");

            accessClientList.remove(userId);
            return setPlaintext(alertEndOfGame.toString());
        }

        //처음 접근이 아님
        if (utterance.equals("문제")) {
            accessClientList.get(userId).initLife();
            accessClientList.get(userId).setCurrentTime();

            accessClientList.get(userId).incrementCurrentIdx();

            questionNumber = accessClientList.get(userId).getCurrentQuestionNumber();
            List<String>addedText = new ArrayList<>();
            addedText.add(getRateText(questionNumber));

            return setQuestion(questionNumber,addedText);
        } else {
            questionNumber = accessClientList.get(userId).getCurrentQuestionNumber();
            if (answer[questionNumber].equals(utterance)) {//정답 맞음
                int tmp = Integer.parseInt(rateCache[questionNumber][0]);
                rateCache[questionNumber][0] = Integer.toString(++tmp);

                accessClientList.get(userId).incrementCurrentIdx();
                accessClientList.get(userId).incrementSuccessTime();
                questionNumber = accessClientList.get(userId).getCurrentQuestionNumber();

                long time = System.currentTimeMillis();
                long seconds = (time / 1000 - accessClientList.get(userId).getSeconds());

                StringBuffer successText = new StringBuffer().append("정답입니다!\n").append("걸린 시간 : ")
                        .append(seconds / 60).append("분").append(seconds % 60).append("초");

                List<String>addedText=new ArrayList<>();
                addedText.add(successText.toString());
                addedText.add(getRateText(questionNumber));

                accessClientList.get(userId).setCurrentTime();

                updateCSV();
                CSVlog();

                return setQuestion(questionNumber,addedText);
            } else {
                int tmp = Integer.parseInt(rateCache[questionNumber][1]);
                rateCache[questionNumber][1] = Integer.toString(++tmp);

                //오답
                int leftLife = accessClientList.get(userId).getLife();
                updateCSV();
                CSVlog();

                if(leftLife <= 0) {
                    StringBuffer failureText = new StringBuffer().append("5번 모두 틀리셨어요ㅠㅠ...\n").append(helpText);
                    return setPlaintext(failureText.toString());
                } else {
                    accessClientList.get(userId).lifeDecrement();
                    StringBuffer alertText = new StringBuffer().append("틀렸습니다. 총 ").append(leftLife)
                            .append("번의 기회가 남았습니다.");

                    return setPlaintext(alertText.toString());
                }
            }
        }

    }

    public HashMap<String,Object>setPlaintext(String returnText) {
        HashMap<String,Object>resultJson = new HashMap<>();

        try {
            List< HashMap<String,Object>>outputs = new ArrayList<>();
            HashMap<String,Object>template = new HashMap<>();
            HashMap<String,Object>simpleText = new HashMap<>();
            HashMap<String,Object>text = new HashMap<>();

            text.put("text",returnText);

            simpleText.put("simpleText",text);
            outputs.add(simpleText);

            template.put("outputs",outputs);

            resultJson.put("version","2.0");
            resultJson.put("template",template);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("error!");
        }
        return resultJson;
    }

    public void readCSV(){
        int row = 0;
        try {
            CSVReader reader = new CSVReader(new FileReader(csvFileName));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                for (int i = 0; i< nextLine.length ; ++i) {
                    rateCache[row][i] = nextLine[i];
                }
                ++row;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateCSV() {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(csvFileName));
            for (int i = 0;i < totalQuestionRow ; ++i) {
                writer.writeNext(rateCache[i]);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}