package com.example.chatbot;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@Slf4j
public class Client {
    private int life;
    private int currentIdx;
    private int successTime;
    private long seconds;
    private List<Integer>questions = new ArrayList<>();

    Client() {
        currentIdx = -1;
        successTime = 0;
        life=5;

        for(int i=0;i<6;++i) {
            questions.add(i);
        }

        Collections.shuffle(questions);
        setCurrentTime();

        System.out.print("shuffled list : ");
        for(int i = 0;i < questions.size();++i){
            System.out.print(questions.get(i)+" ");
        }
        System.out.println();
    }

    public int getSuccessTime() {
        return successTime;
    }

    public void incrementSuccessTime(){
        ++successTime;
    }

    public void setCurrentTime(){
        long time = System.currentTimeMillis();
        log.info("time = " + time);
        seconds = time / 1000;
        log.info("real time = " + this.getSeconds());
    }

    public void incrementCurrentIdx(){
        ++currentIdx;
    }

    public int getCurrentQuestionNumber(){
        return questions.get(currentIdx);
    }

    void initLife(){
        life=5;
    }

    public int getPureCurrentQuestionNumber(){
        return currentIdx;
    }

    public int getLife(){
        return life;
    }

    public void lifeDecrement(){
        --life;
    }

    public long getSeconds(){
        return seconds;
    }
}
