package frc.team_8840_lib.libraries;

public class LibraryInfo {
    public String name;
    public String version;
    public String author;
    public String description;
    public String repo; //EX: frc8840/8840-utils

    public boolean experimental = false;

    //Names that can be found on library website.
    public String[] dependencies;
    //GitHub project names (ex: frc8840/8840-utils)
    public String[] thirdPartyDependencies;

    public LibraryInfo(String name, String version, String author, String description, String[] dependencies, String[] thirdPartyDependencies) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.dependencies = dependencies;
        this.thirdPartyDependencies = thirdPartyDependencies;
    }

    public LibraryInfo() {}
}
