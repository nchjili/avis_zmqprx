AVIS_PREFIX=/usr
AVIS_SRC=/usr/src/avis-client-1.2.4/
AVIS_PLATFORM=platforms/gnu_automake/

all: avis_listener avis_hello_world

avis_listener: avis_listener.c
	gcc -I "${AVIS_SRC}/${AVIS_PLATFORM}" -I "${AVIS_PREFIX}/include/" avis_listener.c "${AVIS_PREFIX}/lib/libavis.a" -lm -o avis_listener

avis_hello_world: avis_hello_world.c
	gcc -I "${AVIS_SRC}/${AVIS_PLATFORM}" -I "${AVIS_PREFIX}/include/" avis_hello_world.c "${AVIS_PREFIX}/lib/libavis.a" -lm -o avis_hello_world

clean:
	-rm -f avis_hello_world avis_listener
