package com.sinlo.core.prototype;

import java.util.List;

public class SampleBean {

    private String name;

    @Prop("The Identity")
    private int id;

    private List<Integer> scores;

    private Long howLong;

    @Prop("The Green Eyed Monster")
    private boolean monster;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Prop("Of course!")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Integer> getScores() {
        return scores;
    }

    public void setScores(List<Integer> scores) {
        this.scores = scores;
    }

    public Long getHowLong() {
        return howLong;
    }

    public void setHowLong(Long howLong) {
        this.howLong = howLong;
    }

    public boolean getTheMonster() {
        return monster;
    }

    public void setTheMonster(boolean monster) {
        this.monster = monster;
    }

    @Prop("A Great Black Dragon!")
    public String getADragon() {
        return "A Dragon";
    }

    public String whatNot() {
        return "what not";
    }
}
