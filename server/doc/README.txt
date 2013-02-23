Avis
======================================================================

This package contains the Avis publish/subscribe message router. For
more information on the Avis project please see:

  http://avis.sourceforge.net/


Installation
----------------------------------------------------------------------

Simply unzip the distribution.


Supported Platforms
----------------------------------------------------------------------

Avis will run on platforms with a Java 5 Standard Edition runtime. It
has been tested on Mac OS X (10.4 and 10.5), Windows XP and Windows
Server 2003, Fedora (Core 2 through 8) and Debian Sarge (3.1).

Although Avis is platform-independent, the "avisd" script and example
command lines appearing later are for Unix environments with a Bourne
shell.  Windows users can either translate as needed or run under
cygwin (http://www.cygwin.com).


Requirements
----------------------------------------------------------------------

Avis requires a Java 1.5 runtime: it will run fine with a minimal Java
Runtime Environment (JRE) but it is recommended you install a full
Java 5 or Java 6 (recommended) JDK to gain access to the "server"
optimizing VM which significantly improves the performance of Avis.

There is no requirement to build from source since platform-
independent binaries are included with the distribution, but if you do
wish to compile Avis you will need a Java Development Kit. If you
don't need to build Avis, you can skip to the next section.

Unless you plan to build Avis from Eclipse, you will also need Apache
Ant 1.6.0 or later (http://ant.apache.org).

Optional:

  * Eclipse 3.2 or later. Project files for Eclipse are included with
    the distribution, import them using File -> Import -> Existing
    Projects Into Workspace. The version of Ant bundled with Eclipse
    is sufficient to build Avis. Eclipse is available from
    http://www.eclipse.org/downloads.

  * JavaCC 4.0 or later. Only required if you wish to change the
    subscription parser. http://javacc.dev.java.net.

To build the router with Ant, change to the "server" sub-directory of
where you extracted Avis and simply run Ant with the default build
target:

  > cd avis-1.2/server
  > ant

This will build the file "lib/avis-router.jar", which is the Avis
event router executable.

To see all build targets run:

  > ant -projecthelp


Usage
----------------------------------------------------------------------

To run the Avis event router service using the bash helper script:

  > cd avis-1.2/server
  > ./bin/avisd

To see command line options:

  > ./bin/avisd -h

You can use the ec (Elvin Consumer) and ep (Elvin Producer) utilities
which are bundled with the router to subscribe to and generate
notifications from the command line.

  [from shell #1]
  > ec -e elvin://localhost "require (Hello-World)"
  ec: Connected to server elvin:4.0/tcp,none,xdr/localhost:2917

  [from shell #2]
  > ep -e elvin://localhost
  Hello-World: 1
  ^D
  ep: Closing connection

  [output on shell #1]
  $time 2007-04-13T20:37:28.156+0930
  Hello-World: 1
  ---

You can also try one of the tickertape messaging clients at
tickertape.org:

  http://tickertape.org/get_tickertape.html
