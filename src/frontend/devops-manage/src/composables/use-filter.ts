export default () => {
  const handleChange = (newStatus: string) => {
    console.log('I am updated', newStatus);
  };

  return {
    handleChange,
  };
};
