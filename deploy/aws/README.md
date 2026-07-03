# AWS deployment

Runs the full stack (app + MySQL + Kafka) on a single EC2 instance with Docker
Compose, pulling the application image from ECR. Suited to demos and low-traffic
environments; production would split the datastores out to RDS/MSK.

## One-time setup

```bash
# Image registry
aws ecr create-repository --repository-name daraja-payments
aws ecr get-login-password | docker login --username AWS \
  --password-stdin <account>.dkr.ecr.<region>.amazonaws.com
docker tag daraja-payments:local <account>.dkr.ecr.<region>.amazonaws.com/daraja-payments:latest
docker push <account>.dkr.ecr.<region>.amazonaws.com/daraja-payments:latest

# Instance role so EC2 can pull from ECR
aws iam create-role --role-name daraja-ec2 --assume-role-policy-document \
  '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
aws iam attach-role-policy --role-name daraja-ec2 \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
aws iam create-instance-profile --instance-profile-name daraja-ec2
aws iam add-role-to-instance-profile --instance-profile-name daraja-ec2 --role-name daraja-ec2

# Security group: SSH from your IP only; 8080 public for the API
aws ec2 create-security-group --group-name daraja-sg --description "daraja-payments"
aws ec2 authorize-security-group-ingress --group-name daraja-sg --protocol tcp --port 22 --cidr <your-ip>/32
aws ec2 authorize-security-group-ingress --group-name daraja-sg --protocol tcp --port 8080 --cidr 0.0.0.0/0
```

## Launch and deploy

```bash
# t3.small (2 GiB) is the practical minimum for app + MySQL + Kafka
aws ec2 run-instances --image-id <al2023-ami> --instance-type t3.small \
  --key-name <keypair> --security-groups daraja-sg \
  --iam-instance-profile Name=daraja-ec2

# On the instance: install docker + compose plugin, then
scp deploy/aws/compose.yml <instance>:~/
scp .env.production <instance>:~/aws.env      # never committed
ssh <instance> 'aws ecr get-login-password --region <region> | \
  docker login --username AWS --password-stdin <account>.dkr.ecr.<region>.amazonaws.com && \
  docker compose -f compose.yml --env-file aws.env up -d'
```

The compose file caps JVM and Kafka heap sizes so the full stack fits in 2 GiB;
add a swapfile for headroom on small instances.

## Cost hygiene

Terminate the instance when it is not needed (the root volume is deleted with
it) and keep an AWS Budget with email alerts as a guard rail:

```bash
aws ec2 terminate-instances --instance-ids <id>
aws budgets create-budget --account-id <account> --budget file://budget.json \
  --notifications-with-subscribers file://notifications.json
```
