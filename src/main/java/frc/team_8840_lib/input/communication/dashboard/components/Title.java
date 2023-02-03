package frc.team_8840_lib.input.communication.dashboard.components;

public class Title extends DashboardComponent {
    private String text;

    public Title(String text) {
        super();

        this.text = text;
    }

    public String getInnerHTML() {
        return this.text;
    }

    public String getTag() {
        return "h3";
    }
}
