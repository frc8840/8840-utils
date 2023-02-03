package frc.team_8840_lib.input.communication.dashboard.components;

public class Text extends DashboardComponent {
    private String text;

    public Text(String text) {
        super();

        this.text = text;
    }

    
    public String getInnerHTML() {
        return this.text;
    }

    public String getTag() {
        return "p";
    }
}
