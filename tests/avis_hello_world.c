/*
 *  Avis Elvin client library for C.
 *
 *  Copyright (C) 2008 Matthew Phillips <avis@mattp.name>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of version 3 of the GNU Lesser General
 *  Public License as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#include <stdio.h>

#include <avis/elvin.h>
#include <avis/arrays.h>

#include <math.h>
int main (int argc, const char *argv [])
{
  const char *uri = argc > 1 ? argv [1] : "elvin://127.0.0.1";
  Elvin elvin;
  Attributes *notification;

  /* Try to connect, and exit if we fail */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 1;
  }

  notification = attributes_create ();

  attributes_set_string (notification, "Greeting", "Hello World");
  attributes_set_int32  (notification, "Number", 42);
  attributes_set_int64  (notification, "dumpass", 400000000011);
  /* messing about */
  Array x;
  array_init(&x,128,1); 
  strcpy(x.items,"dabguytreww");
  int lalala = 9;
  memcpy(x.items+50,&lalala,1);
  attributes_set_opaque  (notification, "Opaque", x);
  attributes_set_real64  (notification, "real", NAN);
  attributes_set_real64  (notification, "real2", 2314.342413);
  /* ..... */
  
  Attributes* notification2 = attributes_create ();
  attributes_set_real64  (notification2, "Opaque", 5.0);

  elvin_send (&elvin, notification);
  elvin_send (&elvin, notification2);

  attributes_destroy (notification);
  attributes_destroy (notification2);

  /* This is redundant in this case, but can't hurt. */
  elvin_close (&elvin);

  return 0;
}
