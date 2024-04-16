# Ecommerce Backend

## Table of Contents
- [Introduction](#introduction)
- [Demo](#demo)
- [Functionalities](#functionalities)
- [Frameworks Used](#frameworks-used)
- [How to Test](#how-to-test)
- [Known Issues](#known-issues)

## Introduction
Welcome to the E-commerce Backend project! This project aims to provide robust backend support for e-commerce websites, focusing primarily on customer interactions.

## Demo
[Demo link](https://www.yozer.me/ecommerce) you can test the server by interacting with Swagger UI. Swagger for this server is designed to be an API interaction tool like Postman rather than a documentation tool.

## Functionalities
The server supports most of the functionality associated with customers in an e-commerce domain. However, it does not support functionality for seller interactions. Below are supported functionalities
- User authentication and authorization
- Product browsing and searching
- Cart management
- Order processing
- Ratings and customer feedback
  
## Frameworks Used
This project makes use of several frameworks and libraries to facilitate its functionality. Notably:
- WebServlets for HTTP handling
- Jackson for JSON serialization and deserialization
- JDBC for database interactions
- CGLib for proxy generation
- javax libraries for various utilities

Additionally, the project includes a homemade EntityManager-like class for database interactions, located at `beans/DbContext`.

## Known Issues
### Cache Incoherency
Elements in the enpoints that return a list of resources won't reflect changes if one of their associated resource is changed, this is because a lack of mapping between the search cache and... anything else really.
