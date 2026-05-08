# GEMA

QrCode website that will allow personalized qr codes for autistics or others syndromes in case they get lost or enter in a crisis.
The QR will contain necessary information/guideline that will help others to support the person in case of emergency

## How It Works

<Describe the main flow of the system in simple steps>

Example:
- User creates a profile with emergency information  
- System generates a QR code linked to the profile  
- QR code is scanned in an emergency  
- Relevant information is displayed instantly  

## Diagram

<C4 model or sequence diagram showing system flow and components>

## Tech Stack

- Java 21 + Spring Boot (backend)
- PostgreSQL
- React + TypeScript (frontend)

## Architecture

The project follows a Hexagonal Architecture (Ports and Adapters) approach. The goal is to isolate business logic from frameworks and external systems.

Core contains domain models, use cases, and ports (interfaces). It represents the business rules and has no dependency on frameworks or infrastructure. Adapters implement external concerns such as database access and external APIs. The web layer exposes the system through REST controllers.

The dependency rule is strict: the core must not depend on adapters, frameworks, or infrastructure. All dependencies point inward toward the core.

## Use Cases

- Creation of emergency QR profiles  
- Quick access to critical personal/medical information  
- Support for individuals with specific conditions or in emergency situations  
- Reliable information sharing with first responders

## Set-up

git clone <Link>
cd <Project name>
cd backend
./mvnw spring-boot:run

cd frontend
npm install
npm run dev

## Access

API: <Swagger or backend URL>  
Frontend: http://localhost:3000  

## Contact

LinkedIn: https://www.linkedin.com/in/gabriel-gerhardt-0a8b852b9/  
Email: mailto:gabrielgerhardt27@gmail.com  
GitHub: https://github.com/Gabriel-Gerhardt
