# 8840 Utils

[![Documentation Status](https://readthedocs.org/projects/8840-utils-docs/badge/?version=latest)](https://8840-utils-docs.readthedocs.io/en/latest/?badge=latest)

**[Team 8840](https://team8840.org) - [Jaiden Grimminck](https://github.com/jaidenagrimminck)**
  
**Java Reference:**
[https://frc8840.github.io/8840-utils/build/docs/javadoc/index.html](https://frc8840.github.io/8840-utils/build/docs/javadoc/index.html)

**Read the Docs:** [https://readthedocs.org/projects/8840-utils-docs/](https://readthedocs.org/projects/8840-utils-docs/)

## What is this?

This is a collection of useful utilities used by our team to make the programming process easier and/or cleaner.  
We recognize that a decent amount of these tools are just reinventing the wheel/already exist, but we wanted to make it on our own to learn what happens and how can we potentially improve it in the future.

## How do I use it?

This section will probably not be updated until the start of the season when we start using the library for development, but if you *really* want to use this (sort of scuffed) library, I recommend that you download the repository and put the `/team_8840_lib` folder in your `/src/main/java/frc` folder.  
Then, you can import the classes you want to use in your code by doing `import frc.team_8840_lib.*;` or `import frc.team_8840_lib.<class_name>;`
  
We also have examples in the `/examples` folder, and they should work.  
  
**IMPORTANT:**  
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

## What's in it?

This library has a lot of stuff in it, so here's an overview of what's in it:

- Swerve Drive for both Falcon 500 and Spark Max (note: not tested for Falcon 500)
- IO Devices
- Simulated Controls
- Logging System
- Listeners
- Path Planning with our own path planner (8840-app)
- Easy communications with NetworkTables
- Custom HTTP Server, hosted on the robot
- NeuralNetwork (note: issues with backpropagation due to some double errors)
- ControllerGroup (note: deprecated, subject to removal)
- Many examples for all of the above, found in the examples folder.

### IO Devices

IO Devices are a way to easily simulate controls and sensors, and to switch between them easily.

Here's an example of how to use IO Devices:

```java
//Make sure the IOAccess is accurate. Methods that have READ_WRITE will be able to be read and written to, while READ will only be able to be read from. READ_WRITE must have a set method and read method, and READ must have a read method and not a set method.
@IOAccess(IOPermission.READ_WRITE)
public class IODevice extends IOLayer {
    private double simulatedValue = 0;

    //SomeDevice does not exist, this is supposed to be what you're trying to simulate
    private SomeDevice device;

    public IODevice(int port) {
        //Include this line in every IODevice.
        //This will make sure that the IOManager knows that this device exists.
        super();

        this.device = new SomeDevice(port);

        //This changes a variable in the IOLayer class that tells the library whether or not the device is real or simulated.
        this.setReal(Robot.isReal());
    }

    //Make sure the IOValue is accurate. This is the type of value that IOManager will write with.
    //The method_type is the type of method that this is. This can be READ or WRITE. For this example, we're using WRITE.
    //Make sure that the name is different from other methods in the class.
    @IOMethod(name = "set value", method_type = IOMethodType.WRITE, value_type = IOValue.DOUBLE)
    public void set(double value) {
        this.simulatedValue = value;
    }
    
    //Make sure the IOValue is accurate. This is the type of value that IOManager will write with.
    //The method_type is the type of method that this is. This can be READ or WRITE. For this example, we're using READ.
    //Make sure that the name is different from other methods in the class.
    @IOMethod(name = "some value", method_type = IOMethodType.READ, value_type = IOValue.DOUBLE)
    public double get() {
        return this.isReal() ? this.device.get() : this.simulatedValue;
    }


    //This is the name of the device. This is what will be displayed in the IOManager. This is also what will be used to identify the device. Note that this is a function, so if you want to difference between devices, you can do that here. We recommend not to change names after the robot has been deployed, since the NT values will persist and will not be updated.
    public String getBaseName() {
        return "IO Device";
    }
}
```

### Logging

The logging system is a way to easily log data to some file, or other place. A NT logger exists in this library, but it needs to be updated to be more efficient.

Here's an example of how to use the logging system:

```java
class SomeImportantLoggingThing implements Loggable {
    private int someValue = 0;
    private double multiplier = 1;

    public SomeImportantLoggingThing() {
        //This will add the logger to the list of loggers that will be logged to.
        Logger.addClassToBeAutoLogged(this);
    }

    //This is an example method that is called outside of the class.
    //This method DOES NOT exist in the Loggable interface, and WILL not be called by the logger.
    public void periodic() {
        this.someValue++;
        this.setMultiplier(this.multiplier + 0.1)
    }

    //This is an example method that is called outside of the class.
    //This method DOES NOT exist in the Loggable interface, and WILL not be called by the logger.
    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    //This method will be called by the logger, and any other method with the AutoLog annotation. Specify the type of return value, and the name of the value.
    @AutoLog(logtype = LogType.INT, name = "some value")
    public int log() {
        return this.someValue;
    }

    //This method will be called by the logger, and any other method with the AutoLog annotation. Specify the type of return value, and the name of the value.
    //This is useful for logging streams of data.
    @AutoLog(logtype = LogType.DOUBLE_ARRAY, name = "multiplier")
    public double logMultiplier() {
        double multiplier = this.multiplier;
        double value = this.someValue;

        //Return the combined array.
        return new double[] {multiplier, value};
    }
}
```

Loggables and IODevices can be combined, look at IOPowerDistribution for an example.

Note: IOPowerDistribution is disabled by default. You can enable it by `IOPowerDistribution.init()` but the robot should be connected to the IOPowerDistribtion by CAN in order for it to work. (We'll fix it in the future, but for now it's disabled by default.)
Only one IOPowerDistribution can be initialized at a time. Please use `IOPowerDistribution.getInstance()` to get the instance of the IOPowerDistribution.

### Swerve Drive

The swerve drive is a way to control a swerve drive. It's pretty simple to use, and it's pretty easy to use. Check out the SwerveDrive example class for information initializing the swerve drive, and funnily enough, check out the AutonomousExample for a better example of how to drive it (you can see which file I used for testing more often). Check out AutonomousExample for how the path planner is implemented into the swerve drive.

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
