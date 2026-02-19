# ========================================================================
#  EMS — Terraform Variables
# ========================================================================

variable "aws_access_key" {
  description = "AWS access key ID"
  type        = string
  sensitive   = true
  default     = ""
}

variable "aws_secret_key" {
  description = "AWS secret access key"
  type        = string
  sensitive   = true
  default     = ""
}

variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "ap-southeast-1"
}

variable "project_name" {
  description = "Project name used for resource naming/tagging"
  type        = string
  default     = "ems"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "prod"
}

# ── Networking ────────────────────────────────────────────────────────────

variable "vpc_cidr" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (RDS)"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

# ── EC2 ───────────────────────────────────────────────────────────────────

variable "instance_type" {
  description = "EC2 instance type for the app server"
  type        = string
  default     = "t3.medium"
}

variable "key_name" {
  description = "Name of the AWS key pair for SSH access"
  type        = string
  default     = "ems-keypair"
}

variable "ami_id" {
  description = "AMI ID for the EC2 instance (Ubuntu 22.04). Leave empty to auto-detect."
  type        = string
  default     = ""
}

# ── RDS (MySQL) ───────────────────────────────────────────────────────────

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "MySQL database name"
  type        = string
  default     = "employee"
}

variable "db_username" {
  description = "MySQL master username"
  type        = string
  default     = "ems_user"
}

variable "db_password" {
  description = "MySQL master password"
  type        = string
  sensitive   = true
  default     = "EmsDB2026Secure!xK9m"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB for RDS"
  type        = number
  default     = 20
}

# ── Application ───────────────────────────────────────────────────────────

variable "docker_registry" {
  description = "Docker registry prefix (e.g. docker.io/monishan8130)"
  type        = string
  default     = "docker.io/monishan8130"
}

variable "jwt_secret" {
  description = "JWT secret for the backend application"
  type        = string
  sensitive   = true
  default     = "EmsProductionJwtSecret2026SuperSecureRandomKey99"
}

variable "allowed_ssh_cidrs" {
  description = "CIDR blocks allowed to SSH into the EC2 instance"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}
