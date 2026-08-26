#!/usr/bin/env bash
# Compila y ejecuta el TP1. Uso: ./run.sh [buffer|historial|all|clean]
set -e

cd "$(dirname "$0")"

case "${1:-all}" in
  clean)
    rm -rf out
    echo "out/ eliminado"
    exit 0
    ;;
esac

echo ">> Compilando..."
javac -d out tp1/*.java
echo ">> OK"

case "${1:-all}" in
  buffer)     java -cp out TestBufferGap ;;
  historial)  java -cp out TestHistorial ;;
  all)
    java -cp out TestBufferGap
    echo
    java -cp out TestHistorial
    ;;
  *)
    echo "Uso: ./run.sh [buffer|historial|all|clean]"
    exit 1
    ;;
esac
