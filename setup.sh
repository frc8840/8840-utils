# easy changing of version:
latest_version="v2023.2.1"
latest_jar="https://github.com/frc8840/8840-utils/releases/download/$latest_version/8840-utils.jar"

# check if the folder libs exist
if [ -d "libs" ]; then
    echo "libs folder already exists. Do you want to delete it and make a new one? (y/n, default: n)"
    read delete_libs
    # if delete libs is y, Y
    if [ "$delete_libs" == "y" ] || [ "$delete_libs" == "Y" ]; then
        echo "Deleting libs folder..."
        rm -rf libs
        echo "Deleted libs folder, making a new one."
        mkdir libs
    fi
else; then
    mkdir libs
    echo "Made library folder 'libs'."
fi
cd libs
sleep 1
echo ""
echo "Downloading version $latest_version of 8840-utils.jar. If this is not the latest version, please update this script."
sleep 3
curl -LJO "$latest_jar"
echo ""
sleep 1
echo "Installing..."
sleep 2
cd ..
echo "Editing build.gradle..."
replacing_line="simulationRelease wpi.sim.enableRelease()"
replaced_line="simulationRelease wpi.sim.enableRelease()\n  implementation fileTree(include: ['*.jar'], dir: 'libs'])"
# actually no clue if this works on windows
if [ "$(uname)" == "Darwin" ]; then
    sed -i '.bak' 's|'"$replacing_line"'|'"$replaced_line"'|g' build.gradle
else; then
    sed -i 's|'"$replacing_line"'|'"$replaced_line"'|g' build.gradle
fi
echo "Finished editing build.gradle. Do you want to run ./gradlew build? (y/n, default: y)"
read run_build
# if run build is y, yes, Y, or empty, run build
if [ -z "$run_build" ] || [ "$run_build" == "y" ] || [ "$run_build" == "Y" ]; then
    echo "Running build..."
    ./gradlew build
fi
echo "Finished installing 8840-utils.jar $latest_version!"
echo "-----------------------------------"
echo "8840-utils is made by Team 8840. You can check them out here: https://team8840.org"
echo "Documentation: https://8840-utils-docs.readthedocs.io/en/latest/"
echo "Javadocs: https://frc8840.github.io/8840-utils/build/docs/javadoc/index.html"
echo "Source Code: https://github.com/frc8840/8840-utils"
echo "-----------------------------------"
