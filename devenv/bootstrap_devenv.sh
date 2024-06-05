#!/usr/bin/env sh

# This script expects to be run from the root directory
# of the checked out git tree

if [ "$0" = "./devenv/bootstrap_devenv.sh" ] ; then
  echo Doing bootstrap of development environment
  echo for Transit EMV Checker
else
  echo The devenv/bootstrap_devenv.sh script must be invoked
  echo from the root directory of the git checkout
  echo with the command line \"sh ./devenv/bootstrap_devenv.sh\"
  return 1 2> /dev/null
  exit 1
fi

# Installation of Android command line tools
# Consult the 'Command line tools only' section of
# https://developer.android.com/studio for the filenames
# of the latest versions of the zip packages for each
# possible development environment host OS.
# Filenames are of the form commandlinetools-${dl_os}-${seq}_latest.zip
# where dl_os=mac/win/linux and seq is a sequence number.
# The links on the page referenced above require clicking
# through a license agreement to download the files but as at
# June 2024 the files can be downloaded via curl
# by adding the filename to the prefix shown as $dl_repo
dl_repo=https://dl.google.com/android/repository

# As at June 2024 this is the value of seq on the first page
# referenced above
seq=11076708

# Work out which OS we are on using uname
uname_osname=$(uname -s)

if [ "$uname_osname" = "Darwin" ] ; then
  dl_os=mac
elif [ "$uname_osname" = "Linux" ] ; then
  dl_os=linux
else
  dl_os=win
fi
zip_file_name=commandlinetools-${dl_os}-${seq}_latest.zip

# The development environment will be stored as a sibling to the
# git sandbox
tec_devenv_dir=../_tec-devenv
if [ -r $tec_devenv_dir\$zip_file_name ] ; then
  echo Required installation already present
fi

mkdir -p $tec_devenv_dir
startdir=$(pwd)
cd $tec_devenv_dir
if [ -r $zip_file_name ] ; then
  echo Using preserved download
else
  echo Downloading $zip_file_name
  curl $dl_repo/$zip_file_name -O
fi

# Some fiddling with the directory structure
# is required to get the SDK working, see:
# https://stackoverflow.com/a/65262939
if [ -r cmdline-tools ] ; then
  rm -rf cmdline-tools
fi
unzip -o $zip_file_name
# The directory structure directly after unzip requires updates
# as describe in the following quote from
# https://developer.android.com/tools/sdkmanager
# >  ...
# >  3. In the unzipped cmdline-tools directory, create a sub-directory called latest.
# >  4. Move the original cmdline-tools directory contents, including the lib directory,
# >  bin directory, NOTICE.txt file, and source.properties file, into the newly created
# >  latest directory. You can now use the command-line tools from this location.
# > ...
mkdir ./latest
mv cmdline-tools/* ./latest
mv ./latest cmdline-tools

yes | ./cmdline-tools/latest/bin/sdkmanager --licenses > cmdline-tools/licenses.txt
./cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0" "platform-tools" "platforms;android-34"
./cmdline-tools/latest/bin/sdkmanager --list_installed
# ./cmdline-tools/latest/bin/sdkmanager --list | grep platform-tools

cd $startdir

