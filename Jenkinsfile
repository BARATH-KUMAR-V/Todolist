pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/BARATH-KUMAR-V/Todolist.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t java-todo-list .'
            }
        }

        stage('Test Application') {
            steps {
                bat 'docker image inspect java-todo-list'
            }
        }
    }
}