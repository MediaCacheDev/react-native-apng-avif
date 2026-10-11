module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import io.github.danielkpl2.apngavif.ApngAvifPackage;',
        packageInstance: 'new ApngAvifPackage()',
      },
      ios: {},
    },
  },
};
