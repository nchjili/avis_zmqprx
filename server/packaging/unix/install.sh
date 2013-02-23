#!/bin/sh

# Avis install script for Unix systems.
#
# Run with -h option for usage

root=$(dirname $0)/root
prefix=/usr/local

usage ()
{
  local NL=$'\x0a'
  
  local help="\
  Usage: $0 [-h|--help] [--prefix dir]$NL\

     -h|--help      : This text$NL\
     --prefix dir   : Set install dir prefix (default is \"/usr/local\")$NL"

  echo "$help" >&2
}

OPTS=`getopt -o h --long prefix:,help -n '$0' -- "$@"`

if [ $? != 0 ] ; then exit 1 ; fi

eval set -- "$OPTS"

while [ $# -gt 0 ]; do
  case "$1" in
    --prefix) prefix=$2 ; shift 2 ;;
    -h|--help) usage ; exit 0 ;;
    --) shift ; break ;;
    *) echo "!error" ; shift 1 ;;
  esac
done

install -dv  -m 0755 -o root -g root $prefix/bin $prefix/libexec/avis

install -Dpv -m 0755 -o root -g root $root/sbin/avisd $prefix/sbin/avisd
install -pv  -m 0755 -o root -g root $root/bin/* $prefix/bin
install -pv  -m 0644 -o root -g root $root/libexec/avis/*.jar \
  $prefix/libexec/avis
install -Dpv -m 0644 -o root -g root $root/etc/avis/avisd.config \
  $prefix/etc/avis/avisd.config
