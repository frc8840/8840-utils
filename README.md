# 8840 Utils

> [!WARNING]
> Much of this project is out of date, feel free to peruse but this project is __discontinued__.

[![Documentation Status](https://readthedocs.org/projects/8840-utils-docs/badge/?version=latest)](https://8840-utils-docs.readthedocs.io/en/latest/?badge=latest)

**[Team 8840](https://team8840.org) - [Jaiden Grimminck](https://github.com/jaidenagrimminck)**
  
**Java Reference:**
[https://frc8840.github.io/8840-utils/build/docs/javadoc/index.html](https://frc8840.github.io/8840-utils/build/docs/javadoc/index.html)

**Read the Docs:** [https://readthedocs.org/projects/8840-utils-docs/](https://8840-utils-docs.readthedocs.io/en/latest/)

**Document with all of our technical details explained:** [https://github.com/frc8840/8840-utils/blob/main/FRC%208840%20-%20Bay%20Robotics%20-%20Technical%20Notebook.pdf](https://github.com/frc8840/8840-utils/blob/main/FRC%208840%20-%20Bay%20Robotics%20-%20Technical%20Notebook.pdf)

**See the web dashboard [here](https://github.com/frc8840/8840-app) (the other half of this project).**

## What is this?

This is a collection of useful utilities used by our team to make the programming process easier and/or cleaner.  
We recognize that a decent amount of these tools are just reinventing the wheel/already exist, but we wanted to make it on our own to learn what happens and how can we potentially improve it in the future.

## What's in it?

This library has a lot of stuff in it, so here's an overview of what's in it:

- Swerve Drive for MK4i Spark Max, CANCoders, and Pigeon IMU/TWO
- IO Devices
- Simulated Controls
- Logging System
- Replay System
- Listeners
- Path Planning with our own path planner (8840-app)
- Easy communications with NetworkTables
- Custom HTTP Server, hosted on the robot
- Many examples for all of the above, found in the examples folder.

## Installation

Requires Java 11 or higher.

Open CMD or Terminal and navigate to the folder your project is in.

Run the following command:

```bash
bash <(curl -s https://raw.githubusercontent.com/frc8840/8840-utils/main/setup.sh)
```

Answer "y" to all questions (except if this is the second time you're running this command, then answer "n" to editing `build.gradle`).

You're done! You can now use the library!

## Manual Installation

If you don't want to use the script, you can manually install the library.

Download the latest release as a JAR, then create a new folder in your project called `libs` and move the JAR into it.

In your `build.gradle` file, in the `dependencies` section, add the following line:

```groovy
implementation fileTree(dir: 'libs', include: ['*.jar'])
```

Run `./gradlew build` in your terminal or CMD, and you're done!

## I installed it. How do I use it now?

In your `Main` class, you will have to have this code in order for it to work:  

```java
class Main {
    private Main() {}

    public static void main(String[] args) {
        //Assign the listener to the robot. See Examples/TankDrive.java for more info.
        frc.team_8840_lib.listeners.Robot.assignListener(new TankDrive());

        //Assign the logger. This line is optional. This will record all files to the default directory, at "~/8840applogs"
        frc.team_8840_lib.info.console.Logger.setWriter(new frc.team_8840_lib.info.console.FileWriter("default"));

        //Start the robot - Don't change this line
        RobotBase.startRobot(frc.team_8840_lib.listeners.Robot::new);
    }
}
```

Check [Main](https://github.com/frc8840/8840-utils/blob/main/src/main/java/frc/team_8840_lib/Main.java) for more info.

Check the [examples](https://github.com/frc8840/8840-utils/blob/main/src/main/java/frc/team_8840_lib/examples) for a few basic examples on how to use the library.

For more in depth documentation, check the [docs](https://frc8840.github.io/8840-utils/build/docs/javadoc/index.html) or the [Read the Docs](https://8840-utils-docs.readthedocs.io/en/latest/).

*Note: The docs are still a work in progress, so there may be some to a lot of missing information.*

## What's coming?

We're still working on the library, so there's a lot of stuff that's coming. Here's a list of what's coming:

- Better communication with driver station
- Better and smoother swerve drive
- Better path planning
- More documentation
- Much more!

## Contributing

If you want to contribute to the library, feel free to make a pull request!
We'll review all issues, complaints, compliments, and more! We'll also review all pull requests and merge them if it's beneficial to any team using the library.  
We'll try and fix any issues that there are since we're also using the library!

## License

This library is licensed under the MIT License, but adheres to the WPI-License as well. See the [LICENSE](https://github.com/frc8840/8840-utils/blob/main/LICENSE) and [WPI-License](https://github.com/frc8840/8840-utils/blob/main/WPILib-License.md) files for more information.

## Credits

This library was made by [Team 8840, Bay Robotics](https://team8840.org)'s programming lead [Jaiden Grimminck](https://github.com/jaidenagrimminck). This library also uses WPILib and any library used by them.
For swerve drive, a great deal of credit goes to Team 364 and Team 3512 for their swerve drive code which was adapted for this library.
Credit is also due to any other libraries that this project uses.

## Contact

If you have any questions, comments, concerns, or anything else, feel free to contact us [here!](https://www.team8840.org/contact) or by messaging us on [Instagram](https://www.instagram.com/bay_robotics/).
