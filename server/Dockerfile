FROM php:7.2.2-apache
RUN docker-php-ext-install mysqli

# 1. Creamos un script de entrada
RUN echo '#!/bin/bash\n\
mkdir -p /var/www/html/uploads/perfiles\n\
chown -R www-data:www-data /var/www/html/uploads\n\
chmod -R 775 /var/www/html/uploads\n\
exec apache2-foreground' > /usr/local/bin/docker-entrypoint.sh

# 2. Le damos permisos de ejecución
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# 3. Le decimos a Docker que use este script al arrancar
ENTRYPOINT ["docker-entrypoint.sh"]

