package frc.team_8840_lib.input.communication.dashboard.modules;

import frc.team_8840_lib.input.communication.dashboard.DashboardModule;
import frc.team_8840_lib.input.communication.dashboard.components.Text;
import frc.team_8840_lib.input.communication.dashboard.components.Title;

public class TestModule extends DashboardModule {
    public TestModule() {
        super();
    }

    public void build() {
        this.startNewComponent();
        
        this.addModule(
            new Title("Module Test"),
            new Text("This is a test module!"),
            new Text("This is confirming that a few components are working!"),
            new Text("Check 8840-app to see if this is working.")
        );
    }
}
