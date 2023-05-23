mkdir libs
cd libs
sleep 1
echo "\n"
echo "Downloading version 2023.2.1 of 8840-utils.jar. If this is not the latest version, please update this script."
sleep 3
curl -LJO 'https://github.com/frc8840/8840-utils/releases/download/v2023.2.1/8840-utils.jar'
echo "\n"
sleep 1
echo "Installing..."
sleep 2
echo "Done!"
echo "\n\n"
cd ..