package frc.team_8840_lib.input.communication.dashboard;

import java.util.ArrayList;

import frc.team_8840_lib.input.communication.dashboard.components.DashboardComponent;

public class DashboardModule {
    private ArrayList<DashboardComponent> modules = new ArrayList<DashboardComponent>(); 

    public DashboardModule() {
        ModuleBuilder.registerModule(this.getClass().getSimpleName(), this);
    }

    public void addModule(DashboardComponent module) {
        modules.add(module);
    }

    public void addModule(DashboardComponent ...module) {
        for (DashboardComponent m : module) {
            modules.add(m);
        }
    }

    public void startNewComponent() {
        this.modules = new ArrayList<DashboardComponent>();
    }

    public ArrayList<DashboardComponent> getComponents() {
        return modules;
    }

    public void build() {
        
    }

    public ArrayList<DashboardComponent> getModules() {
        return modules;
    }
}
