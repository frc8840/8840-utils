let currentStep = 1;

let cancoderInversed = false;
let ports_config = {};

let angleOffsets = {};

function showOnlyCurrentStep() {
    const configSectionContainer = document.querySelector(".config-sections");
    for (let i = 0; i < configSectionContainer.children.length; i++) {
        const configSection = configSectionContainer.children[i].querySelector(".config-section-content");

        if (!configSection.classList.contains("config-section-content-hidden")) {
            configSection.classList.add("config-section-content-hidden");
        }

        if (i + 1 == currentStep) {
            configSection.classList.remove("config-section-content-hidden");
        }
    }
}

async function nextStep() {
    const request = await fetch("/api/swerve/config/next-step", {
        method: "GET"
    })

    if (request.status != 200) {
        alert("There was an issue with the robot and we are unable to complete the request.")
    } else {
        currentStep++;
        showOnlyCurrentStep();

        if (currentStep == 2 || currentStep == 7) {
            let waitForTeleop = setInterval(async () => {
                if (await teleopEnabled()) {
                    clearInterval(waitForTeleop);

                    nextStep();
                }
            }, 500);
        } else if (currentStep == 4 || currentStep == 9) {
            let waitForTeleop = setInterval(async () => {
                if (!await teleopEnabled()) {
                    clearInterval(waitForTeleop);

                    if (currentStep == 9 && redoPositionsFlag) {
                        redoPositionsFlag = false;
                        tellRobotRedoPositions();
                        currentStep = 6;
                        showOnlyCurrentStep();
                        return;
                    }

                    nextStep();

                    if (currentStep == 10) {
                        showCode();
                    }
                }
            }, 500);
        } else if (currentStep == 5) {
            let waitForTestFinished = setInterval(async () => {
                const request = await fetch("/api/swerve/config", {
                    method: "GET"
                })

                if (request.status == 200) {
                    const json = await request.json();

                    if (json.data.finished_test) {
                        clearInterval(waitForTestFinished);

                        cancoderInversed = json.data.encoder_inversed;

                        nextStep();
                    }
                }
            }, 500)
        }
    }
}

async function teleopEnabled() {
    const request = await fetch("/api/swerve/config", {
        method: "GET"
    })

    if (request.status == 200) {
        const json = await request.json();

        return json.teleop;
    } else {
        alert("There was an error getting information from the robot!");
        return false;
    }
}

async function onPortsConfirm() {
    const ports = {
        frontLeft: {
            drive: parseInt(document.getElementById("top-left-drive").value),
            turn: parseInt(document.getElementById("top-left-turn").value),
            encoder: parseInt(document.getElementById("top-left-encoder").value)
        },
        frontRight: {
            drive: parseInt(document.getElementById("top-right-drive").value),
            turn: parseInt(document.getElementById("top-right-turn").value),
            encoder: parseInt(document.getElementById("top-right-encoder").value)
        },
        backLeft: {
            drive: parseInt(document.getElementById("bottom-left-drive").value),
            turn: parseInt(document.getElementById("bottom-left-turn").value),
            encoder: parseInt(document.getElementById("bottom-left-encoder").value)
        },
        backRight: {
            drive: parseInt(document.getElementById("bottom-right-drive").value),
            turn: parseInt(document.getElementById("bottom-right-turn").value),
            encoder: parseInt(document.getElementById("bottom-right-encoder").value)
        }
    }

    ports_config = ports;

    const request = await fetch("/api/swerve/config", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({ports})
    })

    if (request.ok) {
        nextStep()
    } else {
        alert("Error!")
    }
}

async function moveToTestPorts() {
    const request = await fetch("/api/swerve/config", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            confirm: true
        })
    })

    if (request.ok) {
        document.querySelector(".test-ports-progress").style = "display: block;"
        document.querySelector(".test-ports-ready").style = "display: none;" 

        const json = await request.json();

        const testing = json.testing;

        //EX: Front Left Drive
        const location = testing.split(" ")[0] + " " + testing.split(" ")[1];
        const motor = testing.split(" ")[2];

        const testLocationText = document.getElementById("test-ports-module-location");;
        const testMotorText = document.getElementById("test-motor-type");

        testLocationText.innerText = location;
        testMotorText.innerText = motor;
    } else {
        alert("There was an error getting the information from the robot!")
    }
}

async function testingPortsUserResponse(correct) {
    const request = await fetch("/api/swerve/config", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            userResponse: correct
        })
    })

    if (request.ok) {
        const json = await request.json();

        if (!json.continue) {
            alert("There was an issue with this process!");
            alert(json.error);
            alert("Please try again, and make sure you are using the correct ports!");
            currentStep = 1;
            showOnlyCurrentStep();
        } else {
            if (json.finished) {
                nextStep();
            } else {
                const testing = json.testing;

                //EX: Front Left Drive
                const location = testing.split(" ")[0] + " " + testing.split(" ")[1];
                const motor = testing.split(" ")[2];

                const testLocationText = document.getElementById("test-ports-module-location");;
                const testMotorText = document.getElementById("test-motor-type");

                testLocationText.innerText = location.toUpperCase();
                testMotorText.innerText = motor.toUpperCase();
            }
        }
    }
}

async function confirmOffsets() {
    const request = await fetch("/api/swerve/config", {
        method: "GET"
    });

    const json = await request.json();

    const recievedOffests = json.data.offsets;

    angleOffsets = recievedOffests;

    nextStep();
}

let redoPositionsFlag = false;

async function redoPositions() {
    redoPositionsFlag = true;
}

async function tellRobotRedoPositions() {
    const request = await fetch("/api/swerve/config", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            redo: true
        })
    })

    if (request.ok) {
        nextStep();
    } else {
        alert("There was an error telling the robot to redo positions!")
    }
}

async function showCode() {
    document.getElementById("code").value = `
    final double maxSpeed = 1.0; //Max speed of the robot (ft/s)
    final double wheelBase = 1.0; //Distance between the wheels (inches)
    final double trackWidth = 1.0; //Distance between the wheels (inches)

    final ModuleConfig topLeft = new ModuleConfig(${ports_config.frontLeft.drive}, ${ports_config.frontLeft.turn}, ${ports_config.frontLeft.encoder}, ${angleOffsets.topLeft});
    final ModuleConfig topRight = new ModuleConfig(${ports_config.frontRight.drive}, ${ports_config.frontRight.turn}, ${ports_config.frontRight.encoder}, ${angleOffsets.topRight});
    final ModuleConfig bottomLeft = new ModuleConfig(${ports_config.backLeft.drive}, ${ports_config.backLeft.turn}, ${ports_config.backLeft.encoder}, ${angleOffsets.bottomLeft});
    final ModuleConfig bottomRight = new ModuleConfig(${ports_config.backRight.drive}, ${ports_config.backRight.turn}, ${ports_config.backRight.encoder}, ${angleOffsets.bottomRight});

    SwerveSettings settings = new SwerveSettings();

    //Set the max speed of the robot. This is used to calculate the max speed of the wheels.
    settings.maxSpeed = new Unit(maxSpeed, Unit.Type.FEET);

    //Set the wheel base and track width of the robot. This is used to calculate the kinematics.
    settings.wheelBase = new Unit(wheelBase, Unit.Type.INCHES);
    settings.trackWidth = new Unit(trackWidth, Unit.Type.INCHES);

    //Update the kinematics with the new settings.
    settings.updateKinematics();

    settings.threshhold = 0.1; //The minimum speed the robot will move at. This is used to prevent the robot from moving when the joystick is not being moved.
    settings.turnThreshhold = 0.1; //The minimum speed the robot will turn at. This is used to prevent the robot from turning when the joystick is not being moved.

    //Create the swerve drive object.
    SwerveDrive swerveDrive = new SwerveDrive(
        topLeft,
        topRight,
        bottomLeft,
        bottomRight,
        new Pigeon(Pigeon.Type.TWO /*OR IMU*/, X /*Insert the CAN ID of the Pigeon*/),
        settings
    );
    `
}

setTimeout(async () => {
    showOnlyCurrentStep();

    let request = await fetch("/api/swerve/config", {
        method: "GET"
    });

    if (request.ok) {
        const json = await request.json();

        if (json.step) {
            if (json.step > 1) {
                alert("You already have a process started! In order to not mess up your robot, please restart the robot and try again!");
                currentStep = 0; 
                showOnlyCurrentStep();
            }
        }
    } else {
        alert("There was an error getting the information from the robot!")
    }
}, 100);