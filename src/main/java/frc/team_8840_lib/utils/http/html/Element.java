package frc.team_8840_lib.utils.http.html;

public class Element {
    private String tag;

    private String content;
    private Element[] children;
    private String[] options;

    private String pre = "";

    public Element(String tag) {
        this.tag = tag;
        children = new Element[0];
        this.options = new String[0];
    }

    public Element(String tag, String[] options) {
        this.tag = tag;
        this.options = options;
        children = new Element[0];
    }

    public Element(String tag, String[] options, Element[] children) {
        this.tag = tag;
        this.options = options;
        this.children = children;
    }

    public Element(String tag, Element[] children) {
        this.tag = tag;
        this.options = new String[0];
        this.children = children;
    }

    public Element setTextContent(String content) {
        this.content = content;
        return this;
    }

    public String getHTML() {
        if (this.content == null) {
            this.content = "";
        }

        String children = "";
        if (this.children.length > 0) {
            for (Element child : this.children) {
                children += child.getHTML();
            }
        }

        String options = "";
        if (this.options.length > 0) {
            for (String option : this.options) {
                options += " " + option;
            }
        }

        return pre + "<" + tag + options + ">" + content + children + "</" + tag + ">";
    }

    public static String Option(String key, String value) {
        return key + "=\"" + value + "\"";
    }

    public static Element CreatePage(Element head, Element body) {
        Element page = new Element("html", new Element[] {head, body});
        page.pre = "<!DOCTYPE html>";
        return page;
    }

    public static Element CreateBody(Element... children) {
        return new Element("body", children);
    }

    public static Element CreateHead(Element... children) {
        return new Element("head", children);
    }
}
