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

/*
 * Listens for notifications from the router with Message and Number fields.
 */
void sub_listener (Subscription *subscription, Attributes *attributes,
                   bool secure, void *user_data)
{
   
  AttributesIter iter;

  printf("------------------\n");
  for (attributes_iter_init(&iter,attributes);
      iter.has_next; attributes_iter_next(&iter) ) {
    const Value* val = attributes_iter_value(&iter);
    const char* name = attributes_iter_name(&iter);
    if (val == NULL) { break; }
    if (val->type == TYPE_STRING) {
      printf("%s (string): %s \n",name,val->value.str);
    } else if (val->type == TYPE_INT32) {
      printf("%s (INT32): %d \n",name,val->value.int32);
    } else if (val->type == TYPE_INT64) {
      printf("%s (INT64): %ld \n",name,val->value.int64);
    } else if (val->type == TYPE_REAL64) {
      if ( isnan(val->value.real64) ) {
        printf("%s (REAL64): NaN\n",name);
      } else {
        printf("%s (REAL64): %f \n",name,val->value.real64);
      }
    } else if (val->type == TYPE_OPAQUE) {
      size_t bytes_size = val->value.bytes.item_count;
      int i;
      printf("%s (OPAQUE): ",name);
      for (i=0; i<bytes_size;i++) {
        printf("%c", (char) ((char*)val->value.bytes.items)[i] );
      }
      printf("\n");
    }
  }

  /* NOTE: it's OK to access client connection from listener callbacks */
}

int main (int argc, const char *argv [])
{
  const char *uri = argc > 1 ? argv [1] : "elvin://127.0.0.1";
  Elvin elvin;
  Attributes *notification;
  Subscription *subscription;

  /* Try to connect, and exit if we fail */
  if (!elvin_open (&elvin, uri))
  {
    elvin_perror ("open", &elvin.error);

    return 1;
  }

  subscription =
    elvin_subscribe (&elvin, "require (Opaque)");

  elvin_subscription_add_listener (subscription, sub_listener, &elvin);

  elvin_event_loop (&elvin);

  elvin_close (&elvin);

  return 0;
}
