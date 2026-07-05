const express = require('express');
const path = require('path');

const app = express();
const PORT = 4200;
const DIST = path.join(__dirname, 'dist', 'proactive-frontend', 'browser');

// Servir les fichiers statiques du build Angular
app.use(express.static(DIST));

// Toutes les routes inconnues → index.html (SPA routing)
app.get('*', (req, res) => {
  res.sendFile(path.join(DIST, 'index.html'));
});

app.listen(PORT, () => {
  console.log(`\n  ➜  ProActive Frontend: http://localhost:${PORT}/\n`);
});
