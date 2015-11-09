#!/bin/sh

# fill these values in
PACKAGE=com.tapnic.biketrackerle
DB=biketrackerle.db

adb backup  -f myAndroidBackup.ab  $PACKAGE
dd if=myAndroidBackup.ab bs=1 skip=24 | python -c "import zlib,sys;sys.stdout.write(zlib.decompress(sys.stdin.read()))" | tar -xf -
cp apps/$PACKAGE/db/$DB $DB
rm myAndroidBackup.ab
rm -rf apps/

# do something with it (need to install sqlitebrowser obviously)
sqliteman $DB
