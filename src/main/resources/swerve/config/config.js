let currentStep = 1;

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

        if (currentStep == 2) {
            let waitForTeleop = setInterval(async () => {
                if (await teleopEnabled()) {
                    clearInterval(waitForTeleop);
                    nextStep();
                }
            }, 500);
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
            alert("There was an issue with this process!")
        }
    }
}

setTimeout(() => {
    showOnlyCurrentStep();
}, 100);