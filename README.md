# cryptowallet

The cryptowallet program enables users to create, manage, and track the performance of crypto wallets, each containing a portfolio of assets.

# Clean and install with tests:
mvn clean install 
# Clean and install skipping tests:
mvn clean install -DskipTests
# Run using in-memory H2 database:
mvn spring-boot:run
# Run using POSTRESQL database in default port 5432 (need to set user and password in application-postgres.properties):
mvn spring-boot:run -Dspring-boot.run.profiles=postgres

# Features:

## Get latest prices:
- A scheduled task that runs every minute fetches the latest prices for all tokens stored in the database.
- The token’s price is then stored in the database.
- The update interval value is stored in the application.properties file (token.price.update.interval)
- API calls to fetch the latest prices are performed concurrently for up to 3 tokens at once using threading.
- Each task’s steps are logged in the console.

## Create a new wallet:
- Users can create wallet by providing an email address.
- The email must be unique.
- If successfully created an empty wallet (with value = 0.0) is returned.
- If a wallet with the same email address already exists in the database, the user is notified.

## Add asset to wallet:
- Users can add assets to their wallets by specifying the symbol, price, and quantity.
- Before adding the asset, the token’s latest price is fetched using the CoinCap API.
- If the price is successfully fetched the asset is added to the wallet.
- If the Token corresponding to the provided symbol (for example, “BTC”) is not yet stored in the database, then its information is fetched and stored in the database (including the up-to-date price) before adding the asset to the wallet.

## Show wallet information:
- Users can consult their wallets.
- A JSON is returned containing the wallet’s id (a unique UUID), the total valued held in the wallet, and the wallet's assets (symbol, price, value).
- If a wallet doesn’t exist for the specified email then the user is notified.

## Wallet evaluation:
- Users can evaluate their wallet’s performance based on the variation of token prices between the current date and a past date.
- The appreciation and depreciation of each token is calculated, and the best and wort performing assets are identified (with their corresponding change percentage).
- The total value of the wallet ate the specified past date.
- It is also possible to use this feature by providing only the wallet’s id and specifying a past date. If this other service is used, the assets are fetched from the database by the wallet’s id instead of using assets provided in the message body.

# Call services

## Create wallet:

### Endpoint: POST /api/wallets/create/{email} or /api/wallets/create (using Body as in example)

Body example:
{
  "email": "experience@email.com"
}

### Output example:
{
    "id": "093aed39-8f26-4178-b84c-0e6fc25c3dd8",
    "total": 0.0
}

## Add Asset to Wallet:

### Endpoints: POST /api/assets/email/{email} or /api/assets/id/{id}

### Body example:
{
  "symbol": "BTC",
  "quantity": 0.5,
  "price": 40000
}

### Output example:
{
    "symbol": "BTC",
    "quantity": 0.5,
    "price": 98014.19335020952,
    "value": 49007.09667510476
}

## Show wallet information:

### Endpoint: GET /api/wallets/email/{email} or /api/wallets/id/{id} or /api/wallets (using Request Body as in example)

### Body example:
{
    "id": "6fe5f7cf-968e-48e7-811c-75682b2d265d"
}

### Output example:
{
    "id": "9e533ca5-1b27-4b37-a66b-6a4eff8b5143",
    "total": 52242.698684080555,
    "assets": [
        {
            "symbol": "BTC",
            "quantity": 0.5,
            "price": 98014.19335020952,
            "value": 49007.09667510476
        },
        {
            "symbol": "ETH",
            "quantity": 1.2,
            "price": 2696.335007479831,
            "value": 3235.602008975797
        }
    ]
}

## Evaluate Wallet performance:

### Endpoint POST /api/wallets/evaluate?date={date}

### Body example:
{
  "assets": [
    {
      "symbol": "BTC",
      "quantity": 0.5,
      "value": 35000
    },
    {
      "symbol": "ETH",
      "quantity": 4.25,
      "value": 15310.71
    }
  ]
}

### Output example:
{
    "total": 60247.57783978655,
    "best_asset": "ETH",
    "best_performance": 32.29,
    "worst_asset": "BTC",
    "worst_performance": -28.09
}

# Note: 
When running mvn clean install, errors appear in /target/generated-souces/annotations that say that
some imports cannot be resolved. These errors disappear after manually opening the files.
Maybe VS Code was not recognizing or indexing the generated sources.  