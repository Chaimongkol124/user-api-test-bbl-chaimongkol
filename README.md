# User API

RESTful API สำหรับจัดการข้อมูลผู้ใช้ (User) ด้วย **Spring Boot 3.5.16** และ **Java 17**
จัดเก็บข้อมูลเป็นไฟล์ JSON บนดิสก์ โดยมีข้อมูลตั้งต้นมาจาก
https://jsonplaceholder.typicode.com/users

## Tech Stack
- Java 17+
- Spring Boot 3.5.16 (spring-boot-starter-web, spring-boot-starter-validation)
- Jackson (อ่าน/เขียนไฟล์ JSON)
- Maven

## โครงสร้างโปรเจกต์
```
user-api/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/userapi/
    │   │   ├── UserApiApplication.java        # main()
    │   │   ├── controller/UserController.java # REST endpoints
    │   │   ├── service/UserService.java       # business logic + การจัดเก็บไฟล์ JSON
    │   │   ├── model/User.java                # data model + validation
    │   │   └── exception/                     # 404 / 400 handling
    │   └── resources/
    │       ├── application.properties
    │       └── users-seed.json                # ข้อมูลตั้งต้น (จาก jsonplaceholder)
    └── test/java/com/example/userapi/
        └── UserApiApplicationTests.java       # integration tests
```

## วิธีรัน
ต้องมี JDK 17+ และต่ออินเทอร์เน็ต (ครั้งแรกเพื่อให้ Maven โหลด dependency)

```bash
# ใช้ Maven ที่ติดตั้งไว้
mvn spring-boot:run

# หรือ build เป็น jar แล้วรัน
mvn clean package
java -jar target/user-api-0.0.1-SNAPSHOT.jar
```

แอปจะรันที่ `http://localhost:8080`

### การจัดเก็บข้อมูล
- เมื่อรันครั้งแรก ระบบจะสร้างไฟล์ `data/users.json` โดยคัดลอกข้อมูลตั้งต้นจาก
  `users-seed.json` (10 รายการ จาก jsonplaceholder)
- ทุกการ Create / Update / Delete จะเขียนทับไฟล์ `data/users.json` ทันที
  ข้อมูลจึงคงอยู่แม้รีสตาร์ทแอป
- เปลี่ยนตำแหน่งไฟล์ได้ที่ `app.data-file` ใน `application.properties`

## API Endpoints

| Method | Path             | คำอธิบาย                       | สำเร็จ          | ไม่พบ |
|--------|------------------|--------------------------------|-----------------|-------|
| GET    | /users           | ดึงรายการผู้ใช้ทั้งหมด          | 200 OK          | -     |
| GET    | /users/{userId}  | ดึงข้อมูลผู้ใช้ตาม id           | 200 OK          | 404   |
| POST   | /users           | สร้างผู้ใช้ใหม่                 | 201 Created     | -     |
| PUT    | /users/{userId}  | แก้ไขข้อมูลผู้ใช้               | 200 OK          | 404   |
| DELETE | /users/{userId}  | ลบผู้ใช้                       | 204 No Content  | 404   |

> POST/PUT จะตรวจสอบความถูกต้อง (validation) หากผิดจะคืน **400 Bad Request**
> โดยฟิลด์ `name`, `username`, `email` จำเป็นต้องมี และ `email` ต้องอยู่ในรูปแบบที่ถูกต้อง

## Data Model (User)
| Field    | Type   | Required |
|----------|--------|----------|
| id       | Long   | กำหนดโดยเซิร์ฟเวอร์ |
| name     | String | ✓ |
| username | String | ✓ |
| email    | String | ✓ (ต้องเป็นอีเมลที่ถูกต้อง) |
| phone    | String | - |
| website  | String | - |

## ตัวอย่างการเรียกใช้ (curl)

```bash
# 1) ดึงทั้งหมด
curl http://localhost:8080/users

# 2) ดึงตาม id
curl http://localhost:8080/users/1

# 2b) id ที่ไม่มีอยู่ -> 404
curl -i http://localhost:8080/users/9999

# 3) สร้างใหม่ -> 201
curl -i -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Ada Lovelace",
        "username": "ada",
        "email": "ada@example.com",
        "phone": "123-456-7890",
        "website": "ada.dev"
      }'

# 4) แก้ไข -> 200 (หรือ 404 ถ้าไม่มี)
curl -i -X PUT http://localhost:8080/users/1 \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Leanne Graham (edited)",
        "username": "Bret",
        "email": "leanne@example.com",
        "phone": "000",
        "website": "leanne.dev"
      }'

# 5) ลบ -> 204 (หรือ 404 ถ้าไม่มี)
curl -i -X DELETE http://localhost:8080/users/1
```

## รัน Test
```bash
mvn test
```

### Test ที่มี
- **`UserApiApplicationTests`** — integration test ครอบคลุม CRUD ทุก endpoint + กรณี 404/400

> ข้อมูลตั้งต้นใน `users-seed.json` ถูกสร้างจากไฟล์ `user-data.json` ที่อัปโหลดมา
> (เช่น website ของ user id=10 คือ `ambrose.net` ตามไฟล์จริง)
