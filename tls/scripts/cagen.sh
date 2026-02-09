#!/bin/bash

# Создаём папку для CA
mkdir -p ca

echo "Генерация приватного ключа CA..."
openssl genrsa -out ca/CAKey.pem 4096

echo "Генерация корневого сертификата CA..."
openssl req -x509 -new -key ca/CAKey.pem -sha512 -days 365 -out ca/CACert.pem \
  -subj "//CN=rabbitmq CA/O=RabbitMQ certgen.sh"

echo "Готово! Сертификаты CA находятся в папке ca/"
ls -la ca/