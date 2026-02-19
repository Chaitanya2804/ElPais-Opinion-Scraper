package com.assignment.scrapping.models;


public class Article {

    private int index;
    private String titleSpanish;
    private String titleEnglish;
    private String content;
    private String imageUrl;
    private String localImagePath;
    private String articleUrl;

    public Article() {}

    public Article(int index) {
        this.index = index;
    }

    // ── Getters & Setters ────────────────────────────

    public int getIndex()                         { return index; }
    public void setIndex(int index)               { this.index = index; }

    public String getTitleSpanish()               { return titleSpanish; }
    public void setTitleSpanish(String t)         { this.titleSpanish = t; }

    public String getTitleEnglish()               { return titleEnglish; }
    public void setTitleEnglish(String t)         { this.titleEnglish = t; }

    public String getContent()                    { return content; }
    public void setContent(String content)        { this.content = content; }

    public String getImageUrl()                   { return imageUrl; }
    public void setImageUrl(String imageUrl)      { this.imageUrl = imageUrl; }

    public String getLocalImagePath()             { return localImagePath; }
    public void setLocalImagePath(String p)       { this.localImagePath = p; }

    public String getArticleUrl()                 { return articleUrl; }
    public void setArticleUrl(String articleUrl)  { this.articleUrl = articleUrl; }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isBlank();
    }

    @Override
    public String toString() {
        return String.format(
                "Article{index=%d, title='%s', hasImage=%b, url='%s'}",
                index, titleSpanish, hasImage(), articleUrl);
    }
}