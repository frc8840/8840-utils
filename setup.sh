mkdir libs
cd libs
echo "Downloading version 2023.2.1 of 8840-utils.jar. If this is not the latest version, please update this script."
curl -o "8840utils.jar" 'https://github.com/frc8840/8840-utils/releases/download/v2023.2.1/8840-utils.jar'
echo "Installing..."
sleep 1
echo "Done!"
cd ..