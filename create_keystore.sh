#!/bin/sh
# Create a release keystore for Focus Timer (run once, save the .jks file and passwords forever).
# Do NOT commit focus-timer-release.jks or your passwords to version control.

KEYSTORE="focus-timer-release.jks"
ALIAS="focus-timer"
VALIDITY=10000

echo "Creating keystore: $KEYSTORE"
echo "You will be asked for a keystore password and a key password (can be the same)."
echo "SAVE THESE PASSWORDS AND THE .jks FILE FOREVER - you need them for every Play Store update."
echo ""

keytool -genkey -v -keystore "$KEYSTORE" -alias "$ALIAS" -keyalg RSA -keysize 2048 -validity $VALIDITY

if [ $? -eq 0 ]; then
  echo ""
  echo "Keystore created: $KEYSTORE"
  echo "1. Move or copy $KEYSTORE to a safe place and back it up."
  echo "2. Copy keystore.properties.example to keystore.properties"
  echo "3. Edit keystore.properties with your storePassword, keyPassword, keyAlias=$ALIAS, and storeFile path to $KEYSTORE"
  echo "4. Never commit keystore.properties or $KEYSTORE to git."
else
  echo "keytool failed. Make sure Java is installed and keytool is on PATH."
fi
