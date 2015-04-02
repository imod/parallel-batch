# parallel-batch


This is a simple spring batch (with spring-boot) project with parallel processing. 

Unfortunate `hello.PersonItemProcessListener.onProcessError(Person, Exception)` is not called in parallel processing - bug in spring batch? 

Issue created at https://jira.spring.io/browse/BATCH-2371
