#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
mingw=false
msys=false
darwin=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  MINGW*)  mingw=true ;;
  MSYS*)   msys=true ;;
  Darwin*) darwin=true
    # Use /usr/libexec/java_home if available, otherwise fall back to /System/Library/Frameworks/JavaVM.framework/Commands/java_home
    if [ -x "/usr/libexec/java_home" ] ; then
      JAVA_HOME=`/usr/libexec/java_home`
    else
      JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home
    fi
    ;;
esac

if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/gentoo-release ] ; then
    JAVA_HOME=`java-config --jre-home`
  fi
fi

if [ -z "$M2_HOME" ] ; then
  # allow symlink
  PRG="$0"

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    case "$link" in
     /*)
       PRG="$link"
       ;;
     *)
       PRG="`dirname "$PRG"`/$link"
       ;;
    esac
  done

  # make it fully qualified
  saved_dir=`pwd`
  cd "`dirname "$PRG"`/.." >&-
  M2_HOME="`pwd -P`"
  cd "${saved_dir}" >&-
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$M2_HOME" ] &&
    M2_HOME=`cygpath --unix "$M2_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# For Darwin, add options to specify how the Application Menu title appears when running with a GUI
if $darwin; then
  M2_MAC_OPTS="-Xdock:name=Maven -Xdock:icon=$M2_HOME/icon.png"
fi

# Setup the command to run
MAVEN_CMD_LINE_ARGS="$@"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

export MAVEN_CMD_LINE_ARGS

# Bootstrap the wrapper
if [ -n "$MVNW_REPOURL" ]; then
  EXEC_ARGS="-Dmaven.repo.local=$MVNW_REPOURL"
fi
EXEC_ARGS="$EXEC_ARGS -Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR:-$(pwd)}"
exec "$JAVACMD" \
  $MAVEN_CMD_LINE_ARGS \
  $M2_MAC_OPTS \
  $EXEC_ARGS \
  -classpath "$M2_HOME/.mvn/wrapper/maven-wrapper.jar" \
  "$WRAPPER_LAUNCHER"