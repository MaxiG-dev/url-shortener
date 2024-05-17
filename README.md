# url-shortener

This is a URL shortener service.

## Services:

### - ms-core

    This is the core service and expose the API and make the initial config.
    Run in the port 8080

### - ms-redirect

    This service is responsible for redirecting the shortened URL to the original URL.
    Run in the port 8081

### - ms-shorten

    This service is responsible for shortening the URL.
    Run in the port 8082

### - ms-delete

    This service is responsible for deleting the shortened URL.
    Run in the port 8083

### - ms-info

    This service is responsible for getting statistics about the shortened URL.
    Run in the port 8084

### - ms-user

    This service is responsible for user management.
    Run in the port 8085

### - ms-auth

    This service is responsible for authentication.
    Run in the port 8086

- Shorten URL
- Redirect to original URL
- Get all shortened URLs
