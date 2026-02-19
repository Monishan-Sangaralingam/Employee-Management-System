# ========================================================================
#  EMS — Terraform Outputs
# ========================================================================

output "ec2_public_ip" {
  description = "Elastic IP of the application server"
  value       = aws_eip.app.public_ip
}

output "ec2_instance_id" {
  description = "EC2 instance ID"
  value       = aws_instance.app.id
}

output "rds_endpoint" {
  description = "RDS MySQL endpoint (host:port)"
  value       = "${aws_db_instance.mysql.address}:${aws_db_instance.mysql.port}"
}

output "rds_hostname" {
  description = "RDS MySQL hostname"
  value       = aws_db_instance.mysql.address
}

output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "app_security_group_id" {
  description = "App security group ID"
  value       = aws_security_group.app.id
}

output "frontend_url" {
  description = "Frontend URL"
  value       = "http://${aws_eip.app.public_ip}"
}

output "backend_url" {
  description = "Backend API URL"
  value       = "http://${aws_eip.app.public_ip}:8090"
}

output "ssh_command" {
  description = "SSH command to access the server"
  value       = "ssh -i ${var.key_name}.pem ubuntu@${aws_eip.app.public_ip}"
}

output "private_key_file" {
  description = "Path to the generated SSH private key"
  value       = local_file.private_key.filename
}
