# 8840 Utils
## Made by [Team 8840](https://team8840.org) 
### Credits: Jaiden Grimminck

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

        //Start the robot - Don't change this line
        RobotBase.startRobot(frc.team_8840_lib.listeners.Robot::new);
    }
}
```
Check [Main](https://github.com/frc8840/8840-utils/blob/main/src/main/java/frc/team_8840_lib/Main.java) for more info.

## What's in it?

### `frc.team_8840_lib.AI`

One of our most important and expanding libraries, this is where we put all of our AI code.  
...Well, "important" since it can run a neural network. DO NOT USE THIS FILE FOR TRAINING. This file is still a work in progress - there's code in a different repo that I'm (Jaiden) working on for the competition that will be added to this folder.  
If you do want to train an AI, refer to our [8840-app](https://github.com/frc8840/8840-app) repository where we do have a working AI training program.

### `frc.team_8840_lib.controllers`

This is where we put all of our speed controller code.
It contains one file, the `ControllerGroup` class. This class was made to be used with a multitude of different speed controllers, but it doesn't really support CAN yet (coming in near future), only PWM.
Refer to `frc.team_8840_lib.examples.TankDrive` for some usage of it.

### `frc.team_8840_lib.examples`
This is where we put all of our examples. 
We only have one example right now, `TankDrive`, which is a simple replica of our code from last year. It's a simple setup using Spark Speed Controllers and a Logitech Joystick to control it. It's one of the most documented files, so if you're looking for a good amount of documentation, this is the place to go.

### `frc.team_8840_lib.info`

This folder is meant for code that has certain info specific to the robot, such as logging to the console or the time. There's a few holes in logic, but it... works out?

#### • `frc.team_8840_lib.info.console`

This has the `Logger` class which is used for logging to the console with important info such as the time. This will be updated in the future to be saved to a file as well as robot info.

#### •`frc.team_8840_lib.info.time`

This has the `TimeKeeper` class, which is used for keeping track of time. It's one of the more important classes, a lot of the other classes use it to figure out the time for the round/game phase/more!
You can also create a subscription to the time, which can call a function before, after, and at a time. See `Examples.TankDrive` to see how it's implemented.

### `frc.team_8840_lib.input`

This is used for input from anything, whether it be a joystick, a sensor, or an HTTP request.

#### • `frc.team_8840_lib.input.communication`

This folder has the `CommunicationManager`, which is used for sending info to the Driver Station through SmartDashboard or other programs. One of the more important files. The folder also has another folder, called `server` which is used for hosting a REST server, which is used in the `CommunicationManager` to host a server on port 5805 (which should be legal according to rule R704 from the 2022 game, Rapid React).

#### • `frc.team_8840_lib.input.controls`

This folder has the `GameController` class. This class is used from getting input from a controller, but only supports the Logitech Extreme 3D controller and a Logitech XBOX Controller. You can implement it in a new class if you want to add a different controller.

### `frc.team_8840_lib.listeners`

This folder contains the `EventListener` abstract class, which is meant to be extended to contain the robot code. 
The folder also has the `Robot` class, which is used for running the `EventListener`. Don't use this class, use the `EventListener` class instead and use `Robot#assignListener` to assign the listener to the robot.
Check out the `TankDrive` example and `Main` class to see how they're implemented. (It also contains the GamePhase enum, which is used for the current phase of the game.)

### `frc.team_8840_lib.utils`

This folder contains all the more general utility classes, mostly enums, interfaces, and some general classes.

#### • `frc.team_8840_lib.utils.controllers`

This folder contains the `EncoderInformation` and `MotorInfo` class which are used to reformat some information from motor/encoder sensors. (A bit useless now, but can be expanded on in the future to provide better info.)
It also contains the `SCType` enum, which is used to specify the type of speed controller. This is used in the `ControllerGroup` class. (I need a better name for it, but yeah.)

#### • `frc.team_8840_lib.utils.controls`

This folder contains all information for the axis's and buttons raw numbers on the controllers.

#### • `frc.team_8840_lib.utils.http`

This contains all the utilities used for creating a path on the REST server. See `CommunicationManager` to see how it's implemented.

#### • `frc.team_8840_lib.utils.interfaces`

This contains all the interfaces used in the library.

#### • `frc.team_8840_lib.utils.math`

This folder contains the `MathUtils` class, which is used for math functions that aren't in the `Math` class.
It also contains the `Matrix` class which is used for matrix math. This is primarily used in the `NeuralNetwork` class.

#### • `frc.team_8840_lib.utils.time`

This folder contains the utilities used with time. It has the `SubscriptionType` enum which is used for registering subscriptions in `TimeKeeper`.
It also contains the `TimeStamp` class which is used primarly for logging.


## What's coming?

We're still working on the library, so there's a lot of stuff that's coming. Here's a list of what's coming:
- CAN support for `ControllerGroup`
- More documentation
- More examples
- A lot more AI code!
- Better communication with the Driver Station
- Being able to be controlled over HTTP
- Much more!

## Contributing

If you want to contribute to the library, feel free to make a pull request!
We'll review all issues, complaints, compliments, and more! We'll also review all pull requests and merge them if it's beneficial to any team using the library.  
We'll try and fix any issues that there are since we're also using the library!

## License

This library is licensed under the MIT License, but adheres to the WPI-License as well. See the [LICENSE](https://github.com/frc8840/8840-utils/blob/main/LICENSE) and [WPI-License](https://github.com/frc8840/8840-utils/blob/main/WPILib-License.md) files for more information.

## Credits

This library was made by [Team 8840, Bay Robotics](https://team8840.org)'s programming lead [Jaiden Grimminck](https://github.com/jaidenagrimminck). This library also uses WPILib and any library used by them.

## Contact

If you have any questions, comments, concerns, or anything else, feel free to contact us [here!](https://www.team8840.org/contact)

# Thanks for reading!