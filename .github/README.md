# GitHub Actions CI/CD Setup

This repository uses GitHub Actions for continuous integration and continuous deployment to Railway.

## Workflows

### Continuous Integration (CI)

The CI workflow (`ci.yml`) runs on each push to the main branch and pull requests targeting the main branch. It:

1. Checks out the code
2. Sets up Docker Buildx
3. Builds the Docker image to verify it builds successfully

### Continuous Deployment (CD)

The CD workflow (`cd.yml`) runs on each push to the main branch and can also be triggered manually. It:

1. Checks out the code
2. Sets up Docker Buildx
3. Installs the Railway CLI
4. Logs in to Railway using the stored token
5. Deploys the application to Railway

## Required Secrets

For these workflows to function properly, you need to set up the following GitHub secret:

- `RAILWAY_TOKEN`: Your Railway API token

### How to Add Secrets

1. Go to your GitHub repository
2. Click on "Settings" > "Secrets and variables" > "Actions"
3. Click "New repository secret"
4. Add the secret name (`RAILWAY_TOKEN`) and value

## Getting a Railway Token

To get a Railway token:

1. Sign in to your Railway account
2. Go to Account Settings
3. Navigate to the "Tokens" section
4. Generate a new token with appropriate permissions
5. Copy the token and add it to your GitHub repository secrets

## Manual Deployment

You can manually trigger the deployment workflow by:

1. Going to the "Actions" tab in your GitHub repository
2. Selecting the "Continuous Deployment" workflow
3. Clicking "Run workflow"