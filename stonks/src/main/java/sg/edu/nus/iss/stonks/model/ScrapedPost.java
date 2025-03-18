package sg.edu.nus.iss.stonks.model;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class ScrapedPost {
    private String redditId; 
    private String title; 
    private String selfText; 
    private String subReddit; 
    private String author; // OP's username
    private int score; // Upvotes - downvotes
    private float upvoteRatio; // Ratio of upvotes to total votes
    private int numComments; 
    private LocalDateTime createdTime; // timeStamp
    private String flairText; // Post flair (e.g., "DD", "Meme")
    private String postUrl; 
    private String ticker; 

    public ScrapedPost() {
    }

    public ScrapedPost(String redditId, String title, String selfText, String subReddit, String author, int score,
            float upvoteRatio, int numComments, LocalDateTime createdTime, String flairText, String postUrl,
            String ticker) {
        this.redditId = redditId;
        this.title = title;
        this.selfText = selfText;
        this.subReddit = subReddit;
        this.author = author;
        this.score = score;
        this.upvoteRatio = upvoteRatio;
        this.numComments = numComments;
        this.createdTime = createdTime;
        this.flairText = flairText;
        this.postUrl = postUrl;
        this.ticker = ticker;
    }

    public String getRedditId() {
        return redditId;
    }

    public void setRedditId(String redditId) {
        this.redditId = redditId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSelfText() {
        return selfText;
    }

    public void setSelfText(String selfText) {
        this.selfText = selfText;
    }

    public String getSubReddit() {
        return subReddit;
    }

    public void setSubReddit(String subReddit) {
        this.subReddit = subReddit;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public float getUpvoteRatio() {
        return upvoteRatio;
    }

    public void setUpvoteRatio(float upvoteRatio) {
        this.upvoteRatio = upvoteRatio;
    }

    public int getNumComments() {
        return numComments;
    }

    public void setNumComments(int numComments) {
        this.numComments = numComments;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public String getFlairText() {
        return flairText;
    }

    public void setFlairText(String flairText) {
        this.flairText = flairText;
    }

    public String getPostUrl() {
        return postUrl;
    }

    public void setPostUrl(String postUrl) {
        this.postUrl = postUrl;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

}
