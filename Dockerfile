# בנייה מרובת-שלבים: שלב ה-build כולל את כל Maven+JDK, שלב ההרצה הסופי נשאר קטן (JRE בלבד, בלי קוד המקור).
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
# קודם רק pom.xml והתלויות - שכבה שנשמרת בין בניות, כך ששינוי קוד לא מוריד מחדש את כל הספריות
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/nadlanitic-0.0.1-SNAPSHOT.jar app.jar
# לא להריץ כ-root בתוך הקונטיינר
RUN useradd --system --no-create-home app
USER app
EXPOSE 8080
# מותאם לשרת חינמי של 512MB ומעבד חלש:
# - עד 50% מהזיכרון לערימה (נמדד: ~70MB בשימוש אמיתי אחרי עומס; Java צריכה עוד ~180MB מחוץ לערימה), GC פשוט שחוסך זיכרון, מחסנית קטנה יותר לכל thread
# - TieredStopAtLevel=1: עלייה מהירה יותר ופחות זיכרון לקומפילציה (מספיק לתנועה נמוכה)
# - ExitOnOutOfMemoryError: אם הזיכרון נגמר - לצאת, ו-Render מפעיל מחדש, במקום להיתקע
# - אזור זמן ישראל: השרתים ב-UTC, ובלי זה "היום" ותזמון משימות הלילה מחושבים לפי שעון אחר
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Jerusalem"
ENTRYPOINT ["java", "-jar", "app.jar"]
