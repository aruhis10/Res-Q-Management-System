Res-Q

Immediate Hazard Mitigation within 30 Minutes. 

Traditional home care services in urban India often face response times of 2 to 4 hours, leaving households vulnerable during critical utility failures. Res-Q is a hyper-local, high-concurrency management platform designed to bridge this "front-line response" gap.
We prioritize stabilization over full resolution, securing premises against burst pipes, sparking sockets, and security breaches before they escalate into major property damage.


Key Features:

--> Hyper-Local Polygon Model: Service zones are divided into 2-3 km "Polygons" to ensure ultra-low latency response. 

--> Micro-Hub Logistics: A network of 200 sq. ft. "Dark Stores" housing the top 100 most common emergency parts for immediate deployment. 

--> Real-Time "Top Box" Tracking: The system monitors inventory levels on technician e-bikes in real-time. 

--> Predictive Demand Forecasting: Uses external factors like weather patterns to preemptively reposition specialists to high-risk hubs. 

--> Flash Fee Billing: A simplified flat-fee structure for rapid, stress-free transactions during emergencies.

📂 System Architecture & Entities
The Res-Q database manages a complex ecosystem of stakeholders and assets: 

Users: Urban residents seeking immediate hazard mitigation. 

Pros: Background-verified, multi-skilled technicians working fixed shifts. 

Inventory: Managed through a Many-to-Many relationship between Micro-Hubs and Parts. 

Operations: Admins use a dedicated dashboard for shift allocation and supply chain restocking based on threshold alerts.

🛠 Tech Stack

Backend: Java (Spring Boot) for high-performance API management and multi-threaded transaction concurrency. 

Database: MySQL, utilizing geospatial functions for polygon management and relational storage for complex service logs. 

Frontend: ReactJS, HTML5, and CSS3 for a responsive, single-tap booking interface. 

Real-Time: JavaScript for live technician tracking on customer maps.


🤝 Project Context

This system was developed as a course project for CSE202: Fundamentals of Database Management Systems at IIIT Delhi. 

Group Members:

Aruhi Sharma 

Snehil Modi 

Vansh Singh
