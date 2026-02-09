#!/bin/bash

while getopts c:o:n: flag
do
    case "${flag}" in
        c) c=${OPTARG};;
        o) o=${OPTARG};;
        n) n=${OPTARG};;
    esac
done

echo "CN=${c}, O=${o}"
echo "file prefix = ${n}"

# Генерация приватного ключа
openssl genrsa -out "${n}Key.pem" 4096

# Создание запроса на подпись (CSR)
openssl req -new -key "${n}Key.pem" -out "${n}.csr" -subj "//CN=${c}/O=${o}"

# Подпись сертификата корневым CA
openssl x509 -req -sha512 -days 365 -in "${n}.csr" \
  -CA ca/CACert.pem -CAkey ca/CAKey.pem -CAcreateserial -out "${n}Cert.pem"

echo "Готово! Созданы файлы:"
echo "  - ${n}Key.pem (приватный ключ)"
echo "  - ${n}Cert.pem (сертификат)"

# Очистка временного файла CSR
rm -f "${n}.csr"
