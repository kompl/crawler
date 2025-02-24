# Crawler

Crawler is a service for extracting page titles from websites. The service accepts a list of URLs and returns, for each URL, either the extracted title or an error message if the title cannot be found or another issue occurs.

## Example Request

You can retrieve page titles using the following `curl` command:

```bash
curl --location --request PUT 'http://0.0.0.0:3002/v1/fetchTitles' \
--header 'Content-Type: application/json' \
--header 'Authorization: ••••••' \
--data '{
    "sites": [
        "https://www.google.com",
        "https://www.wikipedia.org",
        "https://www.stackoverflow.com",
        "https://www.reddit.com",
        "https://www.microsoft.com",
        "https://www.apple.com",
        "https://www.amazon.com",
        "https://www.netflix.com",
        "https://www.nytimes.com"
    ]
}'
```

## Example Response

```json
[
  {"url": "https://www.google.com", "result": {"title": "Google", "type": "Success"}},
  {"url": "https://www.wikipedia.org", "result": {"title": "Wikipedia", "type": "Success"}},
  {"url": "https://www.stackoverflow.com", "result": {"title": "Newest Questions - Stack Overflow", "type": "Success"}},
  {"url": "https://www.reddit.com", "result": {"title": "Reddit - Dive into anything", "type": "Success"}},
  {"url": "https://www.microsoft.com", "result": {"title": "Your request has been blocked. This could be due to several reasons.", "type": "Success"}},
  {"url": "https://www.apple.com", "result": {"title": "Apple", "type": "Success"}},
  {"url": "https://www.amazon.com", "result": {"error": "Title not found", "type": "Failure"}},
  {"url": "https://www.netflix.com", "result": {"error": "Unexpected status code: 403 Forbidden", "type": "Failure"}},
  {"url": "https://www.nytimes.com", "result": {"error": "Title not found", "type": "Failure"}}
]
```
In addition to the standard POST method for retrieving results, you can use the PUT method, which returns information as a stream. This is particularly useful when handling long-running requests, where results are processed and sent as they become available, without waiting for the processing of all items to complete.

## Configuration

The configuration files are located in the `/app/config` directory. A default YAML configuration file looks like this:

```yaml
api:
   host: 0.0.0.0
   port: 3002
   secret: dsasdqwd
threads_pool_size: 80
```

## Running Locally

To run the service locally using Docker Compose, follow these steps:

1. **Ensure Docker and Docker Compose are installed.**
2. **Clone the project repository.**
3. **In the root directory of the project, execute:**

   ```bash
   docker-compose up
   ```

The service will then be available at [http://0.0.0.0:3002](http://0.0.0.0:3002).

---

Feel free to open an issue if you have any questions or need further assistance.
