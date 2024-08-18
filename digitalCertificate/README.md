# Лабораторная работа 7: Создание и использование цифровых сертификатов

## Постановка задачи

Познакомиться с процессом создания иерархии цифровых сертификатов, включая корневой сертификат, промежуточный сертификат и конечный (leaf) сертификат. В процессе работы необходимо использовать основные команды OpenSSL, необходимые для создания ключевых пар, формирования запросов на сертификаты (CSR), подписания сертификатов, а также проверки цепочки доверия между сертификатами.

## Описание решения

Для решения задачи предлагается выполнение следующих шагов:

### 1. Создание ключевой пары для корневого центра сертификации (Root CA)

Сгенерируем ключевую пару с использованием алгоритма ED448 и сохраним её в файл `root_keypair.pem`.

```
openssl genpkey -algorithm ED448 -out root_keypair.pem
```

### 2. Создание запроса на сертификат (CSR) для корневого центра сертификации

Создадим запрос на сертификат (CSR) для Root CA, указав в качестве имени субъекта `"/CN=ROOT CA"`.

```
openssl req -new -subj "/CN=ROOT CA" -addext "basicConstraints=critical,CA:TRUE" -key root_keypair.pem -out root_csr.pem
```

### 3. Просмотр содержимого CSR

Для проверки корректности содержимого запроса на сертификат, используем команду:

```
openssl req -in root_csr.pem -noout -text
```

### 4. Создание и самоподпись корневого сертификата:

Подпишем корневой сертификат собственным ключом (self-signed) и установим срок действия сертификата на 10 лет.

```
openssl x509 -req -in root_csr.pem -signkey root_keypair.pem -days 3650 -out root_cert.pem
```

### 5. Просмотр содержимого корневого сертификата

Проверим содержание и параметры созданного сертификата:

```
openssl x509 -in root_cert.pem -noout -text
```

### 6. Создание ключевой пары для промежуточного центра сертификации (Intermediate CA)

Сгенерируем ключевую пару для Intermediate CA и сохраним её в файл `intermediate_keypair.pem`.

```
openssl genpkey -algorithm ED448 -out intermediate_keypair.pem
```

### 7. Создание запроса на сертификат для промежуточного центра сертификации

Сформируем CSR для Intermediate CA с указанием имени субъекта `"/CN=INTERMEDIATE CA"`.

```
openssl req -new -subj "/CN=INTERMEDIATE CA" -addext "basicConstraints=critical,CA:TRUE" -key intermediate_keypair.pem -out intermediate_csr.pem
```

### 8. Подпись промежуточного сертификата корневым сертификатом

Подпишем сертификат Intermediate CA с использованием корневого сертификата и ключа, указав расширения для не-листового сертификата.

```
openssl x509 -req -in intermediate_csr.pem -CA root_cert.pem -CAkey root_keypair.pem -extfile extensions.cnf -extensions nonLeaf -days 3650 -out intermediate_cert.pem
```

### 9. Просмотр содержимого промежуточного сертификата

Проверим параметры созданного сертификата:

```
openssl x509 -in intermediate_cert.pem -noout -text
```

### 10. Создание ключевой пары для конечного сертификата (Leaf)

Сгенерируем ключевую пару для конечного сертификата и сохраним её в файл `leaf_keypair.pem`.

```
openssl genpkey -algorithm ED448 -out leaf_keypair.pem
```

### 11. Создание запроса на сертификат для конечного субъекта (Leaf)

Сформируем CSR для конечного сертификата с указанием имени субъекта "/CN=LEAF".

```
openssl req -new -subj "/CN=LEAF" -addext "basicConstraints=critical,CA:FALSE" -key leaf_keypair.pem -out leaf_csr.pem
```

### 12. Подпись конечного сертификата промежуточным сертификатом

Подпишем сертификат для конечного субъекта с использованием промежуточного сертификата и ключа, указав соответствующие расширения.

```
openssl x509 -req -in leaf_csr.pem -CA intermediate_cert.pem -CAkey intermediate_keypair.pem -extfile extensions.cnf -extensions Leaf -days 3650 -out leaf_cert.pem
```

### 13. Просмотр содержимого конечного сертификата

Проверим содержание и параметры конечного сертификата:

```
openssl x509 -in leaf_cert.pem -noout -text
```

### 14. Проверка цепочки сертификатов

Проведем проверку цепочки доверия от конечного сертификата до корневого с использованием команды openssl verify.

```
openssl verify -verbose -show_chain -trusted root_cert.pem -untrusted intermediate_cert.pem leaf_cert.pem
```
