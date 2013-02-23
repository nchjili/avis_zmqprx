This is a generic Avis install package for Unix systems not directly
supported with native installation packages.

Avis requires a Java 1.5.0 (or later) runtime. See
http://java.sun.com/javase/downloads.

Installation
----------------------------------------------------------------------

You can install the files directly to /usr/local using the installer
script:

  > sudo ./install.sh --prefix=/usr/local

Or, using GNU stow (http://www.gnu.org/software/stow):

  # install Avis into stow area
  > AVIS_HOME=/usr/local/stow/avis-1.2.0

  > sudo mkdir $AVIS_HOME
  > sudo ./install.sh --prefix=$AVIS_HOME

  # install Avis using stow
  > (cd $AVIS_HOME/.. && sudo stow $(basename $AVIS_HOME))

This way is recommended, since it allows safe, easy uninstall of Avis
with:

  > (cd $AVIS_HOME/.. && sudo stow --delete $(basename $AVIS_HOME))

Running The Avis Service
----------------------------------------------------------------------

Avis will run without any configuration required, but if you do want
to change any of the router parameters a config file template is
installed in:

  /usr/local/etc/avis/avisd.config.

You can leave the config here, or copy it wherever you would prefer to
keep the config file.

Run Avis with:

  > /usr/local/sbin/avisd

Or, if you have a customised configuration:

  > /usr/local/sbin/avisd -c /usr/local/etc/avis/avisd.config


Manual Uninstallation
----------------------------------------------------------------------

An uninstall target is not provided in the script since the author
doesn't want to risk damaging your system. If you don't use stow to
manage installation/uninstallation, the files installed can be found
by prepending the install prefix (default is /usr/local) to each line
of the output of the the following command in the directory you
untarred the installation package:

  > (cd root && find)
