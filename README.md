avis\_zmqprx
===========

Proxy between Avis and 0mq middlewares. This proxy allows you to subscribe/publish to/from avis server using 0mq.

This is a simple fork of avis-router-1.2.2 which adds 0mq support. Also, support for OS X and Windows has been removed as well as RPM packaging (should you need this you will need to modify ant build config, build.xml from original avis-router-1.2.2 might be handy).

NOTE: All avis notifications are passed to all 0mq subscribers, 0mq subscribes should always subscribe with an empty filter

Author
------
Lukas Vacek <lucas.vacek@gmail.com>

License
-------
This program is free software; you can redistribute it and/or  
modify it under the terms of version 3 of the GNU Lesser General  
Public License as published by the Free Software Foundation.  

This program is distributed in the hope that it will be useful,  
but WITHOUT ANY WARRANTY; without even the implied warranty of  
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU  
General Public License for more details.  

You should have received a copy of the GNU General Public License  
along with this program. If not, see <http://www.gnu.org/licenses/>.  

Installation
-------------
If you are running *Linux on x86 or x86-64* you only need Java JRE1.5 (SE) and then:
* clone this repo
* get *avis-src-1.2.2zmq\_prx-linux-x86.zip* or *avis-src-1.2.2zmq\_prx-linux-x86\_64.zip* (depending on your platform) from the repo root
* unzip that
* run `bin/avisd`

Configuration
-------------
* see original avis documentation here: <http://avis.sourceforge.net/documentation.html>
* avis-zmqprx introduces two new options in etc/avisd.config
    * zmq\_address - address which zmq should bind (or connect) to
    * zmq\_bind - true: bind to zmq\_address, false: connect to zmq\_address
* *If you want to run 0mq against already deployed avis router*, you can set up fedaration between the avis router and avis-zmqprx (check out avis documentation for details)

Running with system-wide zmq
----------------------------
* Because zmq and jzmq are only platform-depend parts of avis-zmqprx you can just delete them from server/lib/ directory and it will pick up your system-wide installation of zmq and jzmq (or alternativelly pass the paths in classpath,-Djava.library.path and LD\_LIBRARY\_PATH (or your platform's alternative) )

Build dependencies
------------------
* POSIX environment
* unzip, tar
* autotools (needed to build jzmq)
* ant
* JRE (>=1.5)
* JDK (>=1.5)
* uuid-dev

Building from source
--------------------
* clone this repo
* make sure you have all build dependencies installed (zmq and jzmq are bundled with the source)
* go to server directory:
* run `ant jar-server` which will build everything and you can run `bin/avisd` then
* alternatively, you can run `ant dist-source` which will create a src package just like the ones available for linux x86 and x86\_64
* run `ant clean` to get clean source tree again

Building from source using system-wide zmq and jmq
--------------------------------------------------
* clone this repo
* make sure you have all build dependencies installed
* ant takes an optional -Dzmq-no-bundle=true parameter to ignore bundled zmq and build against system-wide installation instead
* thus you can run, for example, `ant -Dzmq-no-bundle=true jar-server`

Rebuilding only bundled zmq and jzmq
------------------------------------
* In case you want to rebuild only zmq and jzmq for your platform without rebuilding complete avis-zmqprx, run following:
* in server directory:
* `ant clean-third-party-from-lib`
* `ant clean-jzmq`
* `ant clean-zmq`
* `ant-copy-third-party-to-lib`

Building on windows
-------------------
You will need to get jzmq and zmq libraries for windows, add them to your classpath and library paths and build with -Dzmq-no-bundle=true and pray (this has never ever been tested, feel free to get in touch should you need help).
